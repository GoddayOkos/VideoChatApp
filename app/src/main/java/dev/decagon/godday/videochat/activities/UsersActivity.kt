package dev.decagon.godday.videochat.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dev.decagon.godday.videochat.R
import dev.decagon.godday.videochat.model.User
import dev.decagon.godday.videochat.viewmodel.UsersViewModel
import dev.decagon.godday.videochat.adapter.UserAdapter
import dev.decagon.godday.videochat.databinding.ActivityUsersBinding

class UsersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUsersBinding
    private lateinit var viewModel: UsersViewModel
    private lateinit var signInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: UserAdapter
    private lateinit var currentUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUsersBinding.inflate(layoutInflater)

        viewModel = ViewModelProvider(this).get(UsersViewModel::class.java)
        setContentView(binding.root)

        supportActionBar?.title = "Users"

        auth = Firebase.auth
        checkActiveUser()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        signInClient = GoogleSignIn.getClient(this, gso)

        auth.currentUser?.let { profile ->
             currentUser = User(profile.displayName, profile.email, profile.photoUrl.toString())
        }

        // Initialize UserAdapter
        adapter = UserAdapter(UserAdapter.OnClickHandler{
            // Define the action to perform when the video icon is clicked
            val channelName = if (currentUser.displayName!! < it.displayName!!) {
                currentUser.displayName + it.displayName
            } else {
                it.displayName + currentUser.displayName
            }
            val intent = VideoCallActivity.newIntent(this, currentUser, channelName, it.displayName!!)
            startActivity(intent)
        })

        // Setup recyclerView
        val recyclerView = binding.recyclerview
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Get and observe all users and pass the user list to the
        // recyclerView adapter
        viewModel.getUsers(auth.currentUser?.email)
        viewModel.users.observe(this) {
            adapter.setContact(it)
        }
    }

    override fun onStart() {
        super.onStart()
        checkActiveUser()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkActiveUser() {
        if (auth.currentUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
    }

    private fun signOut() {
        auth.signOut()
        signInClient.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

}