import java.io.File

// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0


agent {
    name = "kb-summarizer"
    description = "Agent that provides weather data."
    var knowledgeSource = ""
    var numberOfTimesFileLoaded = 0
    init = {
        knowledgeSource = File("src/test/resources/knowledge-source.txt").readText()
        numberOfTimesFileLoaded++
    }
    systemPrompt = {
        """
       You are a professional document summarizer.
       You have access to the knowledge file. The knowledge content is as follows:
       $knowledgeSource
       Keep your answer short and concise.
       if you cannot help the user, simply reply "I cant help you"
       Number of times file loaded = $numberOfTimesFileLoaded
     """
    }
    filterInput {
        "A transformed question" replaces "A question"
        -"Bad Word"
        -"Malware.*".toRegex()
    }
    filterOutput {
        -"Bad Word"
        -"Malware.*".toRegex()
    }
}