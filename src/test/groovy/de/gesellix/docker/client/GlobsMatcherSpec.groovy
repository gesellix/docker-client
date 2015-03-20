package de.gesellix.docker.client

import org.apache.commons.lang.SystemUtils
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.PatternSyntaxException

class GlobsMatcherSpec extends Specification {

  def "matches all patterns"() {
    given:
    def matcher = new GlobsMatcher(["abc", "cde"])

    expect:
    matcher.matchers.size() == 2
    and:
    matcher.matches(new File(""), new File("cde"))
    and:
    matcher.matches(new File(""), new File("abc"))
  }

  @Unroll
  def "#pattern should match #path"(String pattern, File base, File path) {
    expect:
    new GlobsMatcher([pattern]).matches(base, path)

    where:
    pattern        | base          | path
    "abc"          | new File(".") | new File("./abc")
    "abc"          | new File("")  | new File("abc")
    "*"            | new File("")  | new File("abc")
    "*c"           | new File("")  | new File("abc")
    "a*"           | new File("")  | new File("a")
    "a*/b"         | new File("")  | new File("abc/b")
    "a*b*c*d*e*/f" | new File("")  | new File("axbxcxdxe/f")
    "a*b*c*d*e*/f" | new File("")  | new File("axbxcxdxexx/f")
    "a*b?c*x"      | new File("")  | new File("abxbbxdbxebxczzx")
    "ab[c]"        | new File("")  | new File("abc")
    "ab[b-d]"      | new File("")  | new File("abc")
    "ab[!c]"       | new File("")  | new File("abd")
    "ab[!e-g]"     | new File("")  | new File("abc")
    "a?c"          | new File("")  | new File("a§c")
    "[a-ζ]*"       | new File("")  | new File("α")
    "[-]"          | new File("")  | new File("-")
  }

  @IgnoreIf({ !SystemUtils.IS_OS_LINUX })
  @Unroll
  def "#pattern should match #path on unix systems"(String pattern, File base, File path) {
    expect:
    new GlobsMatcher([pattern]).matches(base, path)

    where:
    pattern  | base         | path
    "[\\-]"  | new File("") | new File("-")
    "[x\\-]" | new File("") | new File("-")
    "[x\\-]" | new File("") | new File("x")
    "[\\-x]" | new File("") | new File("x")
//    "[\\]a]" | new File("") | new File("]")
  }

  @Unroll
  def "#pattern should not match #path"(String pattern, File base, File path) {
    expect:
    !new GlobsMatcher([pattern]).matches(base, path)

    where:
    pattern        | base         | path
    "a*"           | new File("") | new File("a/bc")
    "a*/b"         | new File("") | new File("a/b/c")
    "a*/b"         | new File("") | new File("a/c/b")
    "a*b*c*d*e*/f" | new File("") | new File("axbxcxdxe/xx/f")
    "a*b*c*d*e*/f" | new File("") | new File("axbxcxdxexx/ff")
    "a*b?c*x"      | new File("") | new File("abxbbxdbxebxczzy")
    "ab[e-g]"      | new File("") | new File("abc")
    "ab[!c]"       | new File("") | new File("abc")
    "ab[!b-d]"     | new File("") | new File("abc")
    "a??c"         | new File("") | new File("abc")
    "a?b"          | new File("") | new File("a/b")
    "a*b"          | new File("") | new File("a/b")
  }

  @IgnoreIf({ !SystemUtils.IS_OS_LINUX })
  @Unroll
  def "#pattern should not match #path on unix systems"(String pattern, File base, File path) {
    expect:
    !new GlobsMatcher([pattern]).matches(base, path)

    where:
    pattern  | base         | path
    "[a-ζ]*" | new File("") | new File("A")
    "[x\\-]" | new File("") | new File("z")
    "[\\-x]" | new File("") | new File("z")
  }

  @Unroll
  def "#pattern should throw exception"(String pattern, File base, File path) {
    when:
    new GlobsMatcher([pattern]).matches(base, path)

    then:
    thrown(PatternSyntaxException)

    where:
    pattern | base         | path
    "[]a]"  | new File("") | new File("]")
  }
}
