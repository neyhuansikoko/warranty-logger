package com.neyhuansikoko.warrantylogger.view

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.neyhuansikoko.warrantylogger.SETTINGS_LOCK_KEY
import com.neyhuansikoko.warrantylogger.databinding.FragmentSettingsBinding
import com.neyhuansikoko.warrantylogger.displayShortMessage

class SettingsFragment : Fragment() {

    private var sharedPreferences: SharedPreferences? = null

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)
        binding.switchSettingsLock.isChecked = sharedPreferences?.getBoolean(SETTINGS_LOCK_KEY, false) ?: false
        
        setupSwitchSettingsLock()
    }

    private fun setupSwitchSettingsLock() {
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.switchSettingsLock.setOnCheckedChangeListener { _, isChecked ->
                    sharedPreferences?.edit()?.let {
                        it.putBoolean(SETTINGS_LOCK_KEY, isChecked)
                        it.apply()
                    }

                    displayShortMessage(
                        binding.switchSettingsLock,
                        "Lock Screen ${if (isChecked) "enabled" else "disabled"}"
                    )
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                binding.switchSettingsLock.isEnabled = false
                binding.tvSettingsLockError.visibility = View.VISIBLE
                binding.tvSettingsLockError.text = "No biometric features available on this device"
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                binding.switchSettingsLock.isEnabled = false
                binding.tvSettingsLockError.visibility = View.VISIBLE
                binding.tvSettingsLockError.text = "Biometric features are currently unavailable"
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.switchSettingsLock.isEnabled = false
                binding.tvSettingsLockError.visibility = View.VISIBLE
                binding.tvSettingsLockError.text = "No biometric features currently setup on this device"
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                binding.switchSettingsLock.isEnabled = false
                binding.tvSettingsLockError.visibility = View.VISIBLE
                binding.tvSettingsLockError.text = "Security update required to use biometric features"
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                binding.switchSettingsLock.isEnabled = false
                binding.tvSettingsLockError.visibility = View.VISIBLE
                binding.tvSettingsLockError.text = "Biometric features are currently not supported"
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                binding.switchSettingsLock.isEnabled = false
                binding.tvSettingsLockError.visibility = View.VISIBLE
                binding.tvSettingsLockError.text = "Biometric features are currently unavailable"
            }
        }
    }
}