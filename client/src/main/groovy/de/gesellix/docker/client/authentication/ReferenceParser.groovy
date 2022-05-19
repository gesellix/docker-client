package de.gesellix.docker.client.authentication

import com.google.re2j.Matcher
import com.google.re2j.Pattern
import groovy.util.logging.Slf4j

/** Grammar (https://github.com/distribution/distribution/blob/main/reference/reference.go)
 *
 * reference                       := name [ ":" tag ] [ "@" digest ]
 * name                            := [domain '/'] path-component ['/' path-component]*
 * domain                          := domain-component ['.' domain-component]* [':' port-number]
 * domain-component                := /([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])/
 * port-number                     := /[0-9]+/
 * path-component                  := alpha-numeric [separator alpha-numeric]*
 * alpha-numeric                   := /[a-z0-9]+/
 * separator                       := /[_.]|__|[-]*{@literal /}//
 * tag                             := /[\w][\w.-]{0,127}/
 *
 * digest                          := digest-algorithm ":" digest-hex
 * digest-algorithm                := digest-algorithm-component [ digest-algorithm-separator digest-algorithm-component ]*
 * digest-algorithm-separator      := /[+.-_]/
 * digest-algorithm-component      := /[A-Za-z][A-Za-z0-9]*{@literal /}
 * digest-hex                      := /[0-9a-fA-F]{32,}/ ; At least 128 bit digest value
 *
 * identifier                      := /[a-f0-9]{64}/
 * short-identifier                := /[a-f0-9]{6,64}/
 */
@Slf4j
class ReferenceParser {

  String domainComponentRegexp = "(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])"

  String separatorRegexp = "(?:[._]|__|[-]*)"

  String nameComponentRegexp = "[a-z0-9]+(?:(?:${separatorRegexp}[a-z0-9]+)+)?"

  String DomainRegexp = "${domainComponentRegexp}(?:(?:\\.${domainComponentRegexp})+)?(?::[0-9]+)?"

  String NameRegexp = "(?:${DomainRegexp}/)?${nameComponentRegexp}(?:(?:/${nameComponentRegexp})+)?"

  String TagRegexp = "[\\w][\\w.-]{0,127}"

  String DigestRegexp = "[A-Za-z][A-Za-z0-9]*(?:[-_+.][A-Za-z][A-Za-z0-9]*)*[:][[:xdigit:]]{32,}"

  String ReferenceRegexp = "^(${NameRegexp})(?::(${TagRegexp}))?(?:@(${DigestRegexp}))?\$"

  String anchoredNameRegexp = "^(?:(${DomainRegexp})/)?(${nameComponentRegexp}(?:(?:/${nameComponentRegexp})+)?)\$"
//    String anchoredNameRegexp = "^((${DomainRegexp})\\/)?(${nameComponentRegexp}(?:(?:\\/${nameComponentRegexp})+)?)\$"

  int NameTotalLengthMax = 255

// Parse parses s and returns a syntactically valid Reference.
// If an error was encountered it is returned, along with a nil Reference.
// NOTE: Parse will not handle short digests.
  def parse(String s) { //(Reference, error) {
    if (!s) {
      throw new IllegalArgumentException("repository name must have at least one component")
    }

    Pattern referencePattern = Pattern.compile(ReferenceRegexp)
    Matcher referenceMatcher = referencePattern.matcher(s)
    if (!referenceMatcher.matches()) {
      if (referencePattern.matches(s.toLowerCase())) {
        throw new IllegalArgumentException("repository name must be lowercase")
      }
      throw new IllegalArgumentException("invalid reference format")
    }

    log.debug("referenceMatcher.groupCount(): ${referenceMatcher.groupCount()}")
    if (referenceMatcher.group(1).length() > NameTotalLengthMax) {
      throw new IllegalArgumentException("repository name must not be more than ${NameTotalLengthMax} characters")
    }

    Map repo = [
        domain: "",
        path  : ""
    ]
    log.debug("anchoredNameRegexp: ${anchoredNameRegexp}")
    Pattern anchoredNamePattern = Pattern.compile(anchoredNameRegexp)
    Matcher anchoredNameMatcher = anchoredNamePattern.matcher(referenceMatcher.group(1))

    log.debug("anchoredNameMatcher.matches(${referenceMatcher.group(1)}): ${anchoredNameMatcher.matches()}")
    log.debug "anchoredNameMatcher.groupCount(): ${anchoredNameMatcher.groupCount()}"

    if (anchoredNameMatcher.matches()
        && anchoredNameMatcher.groupCount() >= 2
        && anchoredNameMatcher.group(2) != null) {
      repo.domain = anchoredNameMatcher.group(1) ?: ""
      repo.path = anchoredNameMatcher.group(2)
    }
    else {
      repo.domain = ""
      repo.path = anchoredNameMatcher.group(1)
    }

    Map ref = [
        repo  : repo,
        tag   : referenceMatcher.group(2),
        digest: ""
    ]
    if (referenceMatcher.group(3) != null) {
      ValidateDigest(referenceMatcher.group(3))
      ref.digest = referenceMatcher.group(3)
    }

    def r = getBestReferenceType(ref)
    if (!r) {
      throw new IllegalArgumentException("repository name must have at least one component")
    }
    return r
  }

  String RepoName(Map repo) {
    return repo.domain == "" ? repo.path : repo.domain + "/" + repo.path
  }

  def getBestReferenceType(Map ref) {
    if (!RepoName(ref.repo as Map)) {
      // Allow digest only references
      if (ref.digest) {
        log.debug("--> digest")
        return ref.digest
      }
      log.debug("--> null (no name, no digest)")
      return null
    }
    if (!ref.tag) {
      if (ref.digest) {
        log.debug("--> repo + digest")
        return [
            repo  : ref.repo,
            digest: ref.digest
        ]
      }
      log.debug("--> repo")
      return ref.repo
    }
    if (!ref.digest) {
      log.debug("--> repo + tag")
      return [
          repo: ref.repo,
          tag : ref.tag
      ]
    }
    log.debug("--> ref")
    return ref
  }

  // why not DigestRegex from above?!
  String DigestRegexp2 = "[a-zA-Z\\d-_+.]+:[a-fA-F\\d]+"
  String DigestRegexpAnchored = "^${DigestRegexp2}\$"

  void ValidateDigest(String s) {
    int i = s.indexOf(':')

    // validate i then run through regexp
    if (i < 0 || i + 1 == s.length() || !s.matches(DigestRegexpAnchored)) {
      throw new IllegalArgumentException("invalid checksum digest format")
    }

    String algorithm = s.substring(0, i).toUpperCase()
    if (!knownAlgorithms.contains(algorithm)) {
      throw new IllegalArgumentException("unsupported digest algorithm")
    }

    // Digests much always be hex-encoded, ensuring that their hex portion will always be size*2
    if (algorithmDigestSizes[algorithm] * 2 != s.substring(i + 1).length()) {
      throw new IllegalArgumentException("invalid checksum digest length")
    }
  }

  List<String> knownAlgorithms = [
      'SHA256',
      'SHA384',
      'SHA512'
  ]

  Map<String, Integer> algorithmDigestSizes = [
      'SHA256': 32,
      'SHA384': 48,
      'SHA512': 64
  ]
}
