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
# Feelink
Feelink is a mobile application designed to help users track, understand, and share their emotional experiences. In today's fast-paced world, emotional well-being often takes a backseat—Feelink provides a simple yet powerful way for users to reflect on their moods, identify patterns, and connect with others who may share similar experiences.

With an intuitive interface, Feelink allows users to log their emotions, add context through text or images, and optionally share their mood events with friends or keep them private. The app fosters a supportive community where users can follow each other's emotional journeys, comment on mood updates, and engage in meaningful conversations.

Beyond tracking moods, Feelink enables direct messaging between users, creating a space for personal and empathetic interactions. Whether it's a moment of joy, frustration, or uncertainty, Feelink provides a safe and expressive environment to navigate one's emotions.

By combining personal reflection with social connection, Feelink aims to promote mental wellness, emotional awareness, and a greater sense of community.

# Features
## Mood Tracking
Add mood events with details like emotional state, reason, and social situation.

Choose from predefined emotional states with expressive icons and colors.

View, edit, or delete mood events at any time.

Optionally attach photos or geolocation data to mood events.

## Mood History & Filters
View past mood events in reverse chronological order.

Filter moods by time range, emotional state, or keywords in descriptions.

View mood events on a map if location data is available.

## Social Features
Create a profile with a unique username.

Search for and view other users' profiles.

Follow other users to stay updated on their mood events.

Control privacy by marking mood events as private or public.

Comment on mood events and view all comments.

*Direct messaging between users for private conversations.*

## Offline Support
Add, edit, or delete mood events while offline.

Automatic synchronization once the device regains connectivity.

## Technology Stack
Platform: Android 

Database: Firebase (for offline support)

Maps & Location: Google Maps API

## Key Features
- Main Activity: First-time entry point to the app which gives the user the choice to either login or create an account.
- Login: Allows user to login using valid username and password.
- Create Account: Allows user to create a new account using name, username, Date Of Birth and Password.
- Add MoodEvent: Allows user who has either logged in or created a new account to add mood events with information such as mood icon (emoji), reason, social situation, location and photograph.
- Feed Manager: The feed manager provides two tabs, ("All Moods" tab) displays all mood events from the database and ("Following Moods" tab) Shows mood events from users that the logged-in user follows.
- Mood Event Adapter: Displays mood events in desired manner and allows viewer of the mood event to expand it and see the mood event in more detail.
- user Profile Page: Each user has a profile page that allows you to view and edit profile, view and edit public/private moods of the logged-in user.
- User Search and Follow Requests: Search for other users and follow request can be sent to connect with others and can also view public moods of other users.
- Location-Based Mood Tracking: allows users to optionally attach locations to mood events and view maps of mood events from their history, followed users, and nearby recent events within 5 km.
- Real-Time Chat Integration: A chat feature that allows users to communicate with others in real time.

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
- *UI Mockups* : https://github.com/cmput301-w25/project-cachecows/wiki/Updated-Mockup-Final
- *UI Storyboard*: https://github.com/cmput301-w25/project-cachecows/wiki/Updated-Storyboard-Sequence-Final
- *UML* : https://github.com/cmput301-w25/project-cachecows/wiki/UML-FINAL
