package dev.decagon.godday.videochat.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dev.decagon.godday.videochat.model.User

class UsersViewModel: ViewModel() {

    private val dbContacts = FirebaseDatabase.getInstance().getReference("Users")

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>>
        get() = _users

    fun addUser(user: User) {
        dbContacts.child(user.email!!.split(".").joinToString("")).setValue(user)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("UsersViewModel", "User added successfully")
                } else {
                    Log.d("UsersViewModel", "Unable to add user")
                }
            }
    }

    // This method is used to retrieve/fetch data from the database in realtime
    // The retrieved data (user details) is stored in a MutableLiveData

    fun getUsers(email: String?) {
        dbContacts.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val users = mutableListOf<User>()
                    for (contactSnapshot in snapshot.children) {
                        val user = contactSnapshot.getValue(User::class.java)
                        user?.let { users.add(it) }
                    }
                    _users.value = users.filter { it.email != email }
                }
            }

            // Needed to be overridden/implemented
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}