package de.gesellix.docker.client.builder

import org.apache.commons.lang3.SystemUtils
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.PatternSyntaxException

class GlobsMatcherSpec extends Specification {

    def "matches all patterns"() {
        given:
        def matcher = new GlobsMatcher(new File(""), ["abc", "cde"])
        matcher.initMatchers()

        expect:
        matcher.matchers.size() == 2
        and:
        matcher.matches(new File("cde"))
        and:
        matcher.matches(new File("abc"))
    }

    @Unroll
    "#pattern should match #path"(String pattern, File base, File path) {
        expect:
        new GlobsMatcher(base, [pattern]).matches(path)

        where:
        pattern        | base          | path
        "abc"          | new File(".") | new File("./abc")
        "abc"          | new File("")  | new File("abc")
        "*"            | new File("")  | new File("abc")
        "*c"           | new File("")  | new File("abc")
        "a*"           | new File("")  | new File("a/bc")
        "a*/b"         | new File("")  | new File("a/b/c")
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

    @Requires({ SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC })
    @Unroll
    "#pattern should match #path on unix systems"(String pattern, File base, File path) {
        expect:
        new GlobsMatcher(base, [pattern]).matches(path)

        where:
        pattern  | base         | path
        "[\\-]"  | new File("") | new File("-")
        "[x\\-]" | new File("") | new File("-")
        "[x\\-]" | new File("") | new File("x")
        "[\\-x]" | new File("") | new File("x")
//    "[\\]a]" | new File("") | new File("]")
    }

    @Requires({ SystemUtils.IS_OS_WINDOWS })
    @Unroll
    "#pattern should match #path on windows systems"(String pattern, File base, File path) {
        expect:
        new GlobsMatcher(base, [pattern]).matches(path)

        where:
        pattern | base         | path
        "bin\\" | new File("") | new File("bin\\foo")
    }

    @Unroll
    "#pattern should not match #path"(String pattern, File path) {
        expect:
        !new GlobsMatcher(new File(""), [pattern]).matches(path)

        where:
        pattern        | path
        "a*/b"         | new File("a/c/b")
        "a*b*c*d*e*/f" | new File("axbxcxdxe/xx/f")
        "a*b*c*d*e*/f" | new File("axbxcxdxexx/ff")
        "a*b?c*x"      | new File("abxbbxdbxebxczzy")
        "ab[e-g]"      | new File("abc")
        "ab[!c]"       | new File("abc")
        "ab[!b-d]"     | new File("abc")
        "a??c"         | new File("abc")
        "!a*b"         | new File("ab")
        "a?b"          | new File("a/b")
        "a*b"          | new File("a/b")
    }

    @Requires({ SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC })
    @Unroll
    "#pattern should not match #path on unix systems"(String pattern, File base, File path) {
        expect:
        !new GlobsMatcher(base, [pattern]).matches(path)

        where:
        pattern  | base         | path
        "[a-ζ]*" | new File("") | new File("A")
        "[x\\-]" | new File("") | new File("z")
        "[\\-x]" | new File("") | new File("z")
    }

    @Unroll
    "#pattern should throw exception"(String pattern, File base, File path) {
        when:
        new GlobsMatcher(base, [pattern]).matches(path)

        then:
        thrown(PatternSyntaxException)

        where:
        pattern | base         | path
        "[]a]"  | new File("") | new File("]")
    }

    @Requires({ SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC })
    @Unroll
    "#pattern should throw exception on unix systems"(String pattern, File base, File path) {
        when:
        new GlobsMatcher(base, [pattern]).matches(path)

        then:
        thrown(PatternSyntaxException)

        where:
        pattern | base         | path
        // On *nix, the backslash is the escape character, so it's invalid to be used at the end of a pattern
        // A trailing backslash is only valid on Windows.
        // We actually differ from the official client's behaviour, which silently ignores invalid patterns.
        "bin\\" | new File("") | new File("bin/foo")
    }

    def "allows pattern exclusions"() {
        expect:
        new GlobsMatcher(new File(""), patterns).matches(path) == shouldMatch

        where:
        patterns                 | path                 | shouldMatch
        ["!ab.c", "*.c"]         | new File("ab.c")     | true
        ["dir", "!dir/ab.c"]     | new File("dir/ab.c") | false
        ["dir/", "!dir/ab.c"]    | new File("dir/ab.c") | false
        ["dir/*", "!dir/ab.c"]   | new File("dir/ab.c") | false
        ["dir/*.c"]              | new File("dir/ab.c") | true
        ["dir/*.c", "!dir/ab.c"] | new File("dir/ab.c") | false
    }

    def "allows pattern exclusions in subdirectories"() {
        expect:
        new GlobsMatcher(new File(""), patterns).matches(path) == shouldMatch

        where:
        patterns                                                      | path                                | shouldMatch
        ["ignorefolder", "!ignorefolder/keepme.txt", "**/ignore.txt"] | new File("ignorefolder/keepme.txt") | false
        ["ignorefolder", "!ignorefolder/keepme.txt", "**/ignore.txt"] | new File("ignorefolder/dropme.txt") | true
    }
}
