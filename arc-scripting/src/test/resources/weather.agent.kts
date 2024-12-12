
// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0


agent {
    name = "weather"
    description = "Agent that provides weather data."
    systemPrompt = {
        """
       You are a professional weather service.
       You have access to real-time weather data with the get_weather function.
       Keep your answer short and concise.
       All you require is the location.
       if you cannot help the user, simply reply "I cant help you"
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