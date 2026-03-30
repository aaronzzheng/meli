package com.example.meli.ui.notification

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.meli.databinding.FragmentNotificationBinding
import com.google.firebase.auth.FirebaseAuth

class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by viewModels()
    private val notificationAdapter = NotificationAdapter(
        onAccept = { notification ->
            viewModel.respondToFriendRequest(
                uid = FirebaseAuth.getInstance().currentUser?.uid,
                notification = notification,
                accept = true
            )
        },
        onDecline = { notification ->
            viewModel.respondToFriendRequest(
                uid = FirebaseAuth.getInstance().currentUser?.uid,
                notification = notification,
                accept = false
            )
        }
    )

    private val tagName = "NotificationLifecycle"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tagName, "NotificationFragment onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(tagName, "NotificationFragment onCreateView")
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        binding.notificationRecyclerView.adapter = notificationAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNotifications()
        binding.buttonCloseNotification.setOnClickListener {
            findNavController().navigateUp()
        }
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        viewModel.loadNotifications(uid)
        viewModel.markAllRead(uid)
    }

    private fun observeNotifications() {
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            notificationAdapter.submitList(notifications)
            binding.notificationEmptyText.visibility =
                if (notifications.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                binding.notificationEmptyText.text = error
                binding.notificationEmptyText.visibility = View.VISIBLE
            }
        }

        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearActionMessage()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(tagName, "NotificationFragment onResume")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        viewModel.loadNotifications(uid)
        viewModel.markAllRead(uid)
    }

    override fun onPause() {
        Log.d(tagName, "NotificationFragment onPause")
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(tagName, "NotificationFragment onDestroyView")
        super.onDestroyView()
        binding.notificationRecyclerView.adapter = null
        _binding = null
    }

    override fun onDestroy() {
        Log.d(tagName, "NotificationFragment onDestroy")
        super.onDestroy()
    }
}
