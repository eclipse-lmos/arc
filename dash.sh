#!/bin/bash

# Iterate through folders starting with "arc-"
for dir in arc-*/ ; do
    # Remove trailing slash for gradle project name
    folder_name="${dir%/}"

    if [ ! -f "$folder_name/gradle.lockfile" ]; then
        echo "Running gradle for $folder_name..."
        ./gradlew "${folder_name}:dependencies" --write-locks
    fi

done

# Iterate through folders starting with "arc-"
for dir in arc-*/ ; do
    # Remove trailing slash for gradle project name
    folder_name="${dir%/}"

    if [ -f "$folder_name/gradle.lockfile" ]; then
      echo "Executing dash for $folder_name..."
      cat "$folder_name/gradle.lockfile" \
      | grep -v '^#' \
      | grep -v '^empty' \
      | grep -oh '^[^=]+' \
      | java -jar org.eclipse.dash.licenses-1.1.1-20250926.055030-612.jar -
     fi

done


