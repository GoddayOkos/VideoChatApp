package dev.decagon.godday.videochat.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dev.decagon.godday.videochat.R
import dev.decagon.godday.videochat.model.User
import dev.decagon.godday.videochat.viewmodel.UsersViewModel
import dev.decagon.godday.videochat.databinding.ActivityLoginBinding
import io.agora.rtm.RtmClient

class LoginActivity : AppCompatActivity() {
    private val TAG = "LoginActivity"
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var signInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: UsersViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent result handler
        resultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                onSignInResult(data)
            }
        }

        viewModel = ViewModelProvider(this).get(UsersViewModel::class.java)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signInButton.setOnClickListener { signIn() }

        // Configure Google Sign in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Initialize signInClient
        signInClient = GoogleSignIn.getClient(this, gso)
        // Initialize FirebaseAuth
        auth = Firebase.auth
    }

    private fun signIn() {
        val signInIntent = signInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    private fun onSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            // Google Sign In was successful, authenticate with Firebase
            val account = task.getResult(ApiException::class.java)
            binding.statusTextview.text = "Welcome ${account?.displayName}"
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            // Google Sign In failed, update UI appropriately
            binding.statusTextview.text = "Google sign in failed"
            Log.w(TAG, "Google sign in failed", e)
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct?.id)
        val credential = GoogleAuthProvider.getCredential(acct?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                // If sign in succeeds the auth state listener will be notified and logic to
                // handle the signed in user can be handled in the listener.
                auth.currentUser?.let { profile ->
                        val user = User(profile.displayName, profile.email, profile.photoUrl.toString())
                        viewModel.addUser(user)
                }
                startActivity(Intent(this, UsersActivity::class.java))
                finish()
            }
            .addOnFailureListener(this) {
                Snackbar.make(binding.root, "Firebase authentication failed", Snackbar.LENGTH_LONG)
                    .show()
            }
    }
}