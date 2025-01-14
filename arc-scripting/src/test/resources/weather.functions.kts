/*
 * // SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

/**
 * My weather function.
 */
function(
    name = "get_weather",
    description = "Returns real-time weather information for any location",
    params = types(string("location", "a city to obtain the weather for."))
) { (location) ->
        """
         The weather is good in $location. It is 20 degrees celsius.
        """
}

function(
    name = "get_forecast",
    description = "Returns real-time weather forecast",
) { """
         The next 4 days will be full of sun!
        """
}
