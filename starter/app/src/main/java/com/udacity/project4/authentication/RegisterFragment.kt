package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.AuthenticationHelper
import com.udacity.project4.R
import com.udacity.project4.databinding.FragmentRegisterBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class RegisterFragment : Fragment() {

    val _viewModel: AuthenticationViewModel by viewModel()
    private lateinit var binding: FragmentRegisterBinding

    private val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_register, container, false
            )
        binding.registerButton.setOnClickListener { signIn() }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val navController = findNavController()

        if (requestCode == AuthenticationActivity.SIGN_IN_RESULT_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                binding.errorMsg.text = context?.getText(R.string.error_happened) ?: ""
                navController.popBackStack()
            }
        }
    }

    private fun signIn() =
        startActivityForResult(
            AuthenticationHelper
                .signIn()
                .setAvailableProviders(
                    providers
                ).build(), AuthenticationActivity.SIGN_IN_RESULT_CODE
        )
}