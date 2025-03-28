# CMPUT 301 W25 - Team CacheCows

## Team Members

| Name        | CCID   | GitHub Username |
| ----------- | ------ | --------------- |
| Ronith Bose | ronith | Ronith2005      |
| Kathan Ashishkumar Shah | kathanas | kathan-205   |
| Snigdha Mehta           | mehta5   | snigdha-0902    |
| Natalie Lysenko | nlysenko | NataliiaLysenko    |
| Fatin Ahmed             | fatin2   | fatin2343       |
| Cyril John Maliakal | cyriljo1 | cyriluoa     |

## Project Description

This app called FeeLink so far allows users to login, create accounts, create mood events, and view them in the feed manager. Each Mood event has a emoji (mood icon) and color associated with it and is displayed with them.

## Key Features

- Main Activity: First-time entry point to the app which gives the user the choice to either login or create an account.
- Login: Allows user to login using valid username and password.
- Create Account: Allows user to create a new account using name, username, DOB and Password.
- Add MoodEvent: Allows user who has either logged in or created a new account to add mood events with information such as mood icon (emoji), reason, trigger, social situation, location and photograph.
- Feed Manager: Keeps track of all mood events that are either being followed by the user ('Their Mood' tab)or made by the user themselves ('My Mood' tab).
- Mood Event Adapter: Displays mood events in desired manner and allows viewer of the mood event to expand it and see the mood event in more detail.
  

## Setup Instructions

1. Firebase Configuration
   
   a. Create a Firestore Database
   - Go to the Firebase Console
   - Create a new project or use an existing one
   - Navigate to Firestore Database and create a new database
   
   b. Configure Firestore Security Rules
   - Add the following security rules to your Firestore database, the rules can be found in rules/rules.txt.
   
   c. Create Firestore Indexes
   - Create the following indexes for the `mood_events` collection:
    - Index 1: Collection `mood_events` with fields `userId` (Ascending), `timestamp` (Descending), `__name__` (Descending)
    - Index 2: Collection `mood_events` with fields `timestamp` (Descending), `userId` (Descending), `__name__` (Descending)

2. App Configuration
- Download the `google-services.json` file from your Firebase project
- Place the `google-services.json` file in the `app` directory of the project

3. Running the App
- Open the `feelink` directory in Android Studio
- Wait for Gradle sync to complete
- Connect an Android device or use an emulator
- Click on the "Run" button to build and install the app
    

## Documentation

- *Wiki Link* : [https://github.com/cmput301-w25/project-cachecows.wiki.git](https://github.com/cmput301-w25/project-cachecows/wiki) 
- *Scrum Board* : https://github.com/orgs/cmput301-w25/projects/54
- *UI Mockups* : Can be found in the wiki
- *UML* : Can be found in the wiki
