Heritage Hunter

Heritage Hunter is an Android application designed to help users discover and log historical places, museums, and landmarks in their vicinity. It uses Firebase for data management and the Google Location Services API to track the user's current location and allow interaction with nearby places.
Features

    Location-based discovery: Discover historical places near your current location.
    Logging visits: Log visits to places when you're within a 50m radius. The app will track visit counts and show whether you’ve already visited a place.
    Add new places: Add new historical places using your current GPS coordinates. Each new place includes a name, category, and your user ID as the first visitor.
    Firebase integration: Data for places and user visits are stored and retrieved from Firebase Firestore.
    Dynamic place list: The list of nearby places dynamically updates as you move around.

Installation

To get started with Heritage Hunter, follow the steps below:
Prerequisites

    Android Studio installed (preferably the latest version).
    A Firebase project with Firestore and Authentication enabled.
    A Google Maps API key (for accessing location services).

Setup Instructions

    Clone the Repository

    bash

git clone https://github.com/yourusername/heritage-hunter.git
cd heritage-hunter

Open the Project in Android Studio

Launch Android Studio and open the cloned project directory.

Set up Firebase

    Add your google-services.json file to the app/ directory.
    Enable Firestore and Firebase Authentication (Google login) in the Firebase Console.
    Add your app's SHA-1 and SHA-256 keys in Firebase settings for authentication.

Add Google Maps API Key

    Open the AndroidManifest.xml file and add your Google Maps API key:

    xml

        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="YOUR_GOOGLE_MAPS_API_KEY"/>

    Run the Application

    After setup, click the Run button in Android Studio to build and run the application on your emulator or connected device.

Usage

    Home Screen: Displays a list of nearby historical places with the number of visits, and the distance from your current location.
    Log Visits: When you’re within 50 meters of a location, a target icon will appear. Click it to log your visit.
    Add New Place: Use the floating action button (FAB) to add a new place. You can enter the name and category, while the GPS coordinates will be automatically filled based on your current location.

Code Structure

    MainActivity.kt: The main entry point of the app. It manages navigation and handles Firebase authentication.
    HomeFragment.kt: Contains the logic for displaying nearby places and handling location updates.
    PlacesAdapter.kt: A RecyclerView adapter that binds place data to the list view.
    Firebase Integration: Firebase Firestore is used for storing place data, and Firebase Authentication handles user login.

Key Files

    MainActivity.kt: Manages the overall app layout and Firebase authentication.
    HomeFragment.kt: Manages the dynamic list of places and user interactions.
    PlacesAdapter.kt: Handles displaying place information in a RecyclerView.
    google-services.json: Firebase configuration (ensure it is added to the project).

Permissions

The app requires the following permissions:

    Location Access:
        ACCESS_FINE_LOCATION
        ACCESS_COARSE_LOCATION

These permissions are used to fetch the user's current location and update nearby places in real-time.
Future Enhancements

    Add maps integration to display places on a map.
    Implement user profiles and badges for visiting specific places.
    Add filtering and search functionality for places.

Contributing

Contributions are welcome! Feel free to submit a pull request or open an issue to report bugs or request features.

    Fork the repository
    Create a new branch (git checkout -b feature-branch)
    Commit your changes (git commit -am 'Add new feature')
    Push to the branch (git push origin feature-branch)
    Open a pull request

License

This project is licensed under the MIT License.
Contact

For any inquiries or support, please contact:

    Email: admin@the-public-historian.com
