package dev.decagon.godday.videochat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dev.decagon.godday.videochat.R
import dev.decagon.godday.videochat.model.User

class UserAdapter(private val onClickHandler: OnClickHandler) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    private var users = mutableListOf<User>()

    inner class UserViewHolder(val view: View) :
        RecyclerView.ViewHolder(view) {
        private lateinit var name: String
        private lateinit var user: User
        private lateinit var email: String
        private var photoUrl: String? = null

        fun bind(user: User) {
            this.name = user.displayName!!
            this.email = user.email!!
            this.photoUrl = user.photoUrl
            this.user = user

            val displayName = view.findViewById<TextView>(R.id.fullName)
            displayName.text = name

            val userEmail = view.findViewById<TextView>(R.id.user_email)
            userEmail.text = email

            val displayPhoto: ImageView = view.findViewById(R.id.profileImage)
            photoUrl?.let {
                Glide.with(displayPhoto.context).load(it).into(displayPhoto)
            }

            val videoIcon: ImageView = view.findViewById(R.id.video_chat_btn)
            videoIcon.setOnClickListener { onClickHandler.onClick(user) }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_item, parent, false)
        return UserViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = users[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = users.size

    // Method for feeding data into the adapter class
    fun setContact(users: List<User>) {
        this.users = users as MutableList<User>
        notifyDataSetChanged()
    }

    class OnClickHandler(val clickListener: (user: User) -> Unit) {
        fun onClick(user: User) = clickListener(user)
    }
}