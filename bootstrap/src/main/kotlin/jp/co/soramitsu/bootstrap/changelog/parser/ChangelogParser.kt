package jp.co.soramitsu.bootstrap.changelog.parser

import groovy.lang.GroovyClassLoader
import jp.co.soramitsu.bootstrap.changelog.ChangelogInterface
import org.springframework.stereotype.Component

/**
 * Changelog Groovy parser
 */
@Component
class ChangelogParser {

    /**
     * Parses given Groovy script
     * @param script - groovy script to parse
     * @throws IllegalArgumentException if script is empty or script class doesn't implement ChangelogInterface
     * or in case of invalid(not compilable) script
     * @return parsed ChangelogInterface instance
     */
    fun parse(script: String): ChangelogInterface {
        if (script.isEmpty()) {
            throw IllegalArgumentException("Cannot parse empty script")
        }
        val scriptClass: Class<Any>
        try {
            scriptClass = GroovyClassLoader().parseClass(script)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid changelog script", e)
        }
        val instance = scriptClass.newInstance()
        if (instance is ChangelogInterface) {
            return instance
        }
        throw IllegalArgumentException(
            "Script class must implement " +
                    ChangelogInterface::class.java.simpleName
        )
    }
}
