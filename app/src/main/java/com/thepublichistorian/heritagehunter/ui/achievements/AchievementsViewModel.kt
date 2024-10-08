package com.thepublichistorian.heritagehunter.ui.achievements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AchievementsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Hier findest du deine Erfolge"
    }
    val text: LiveData<String> = _text
}