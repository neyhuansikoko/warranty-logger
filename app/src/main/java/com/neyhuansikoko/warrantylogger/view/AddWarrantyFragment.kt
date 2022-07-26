package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.databinding.FragmentAddWarrantyBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModelFactory
import java.io.File

class AddWarrantyFragment : Fragment() {

    private var _binding: FragmentAddWarrantyBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val navigationArgs: AddWarrantyFragmentArgs by navArgs()

    private lateinit var warranty: Warranty

    private var expirationDateInMillis: Long = 0

    private val sharedViewModel: WarrantyViewModel by activityViewModels {
        WarrantyViewModelFactory((activity?.applicationContext as WarrantyLoggerApplication).database.warrantyDao())
    }

    private var isUsed: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAddWarrantyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val warrantyId = navigationArgs.id

        if (warrantyId > 0) {
            sharedViewModel.getWarrantyById(warrantyId).observe(viewLifecycleOwner) {
                it?.let {
                    warranty = it
                    bind(warranty)

                    binding.apply {
                        btnAddSave.setOnClickListener {
                            if (!isTextFieldEmpty()) {
                                isUsed = true
                                updateWarranty()
                            }
                        }

                        btnAddDelete.setOnClickListener {
                            showConfirmationDialog()
                        }
                        btnAddDelete.visibility = View.VISIBLE

                        btnAddTakePicture.setOnClickListener {
                            navigationArgs.imageUri?.let { uriString ->
                                //Delete old image
                                deleteFileByUri(uriString.toUri(), requireActivity())
                            }
                            val action = AddWarrantyFragmentDirections.actionAddWarrantyFragmentToCameraFragment(id = warrantyId)
                            findNavController().navigate(action)
                        }
                    }
                }
            }
        } else {
            binding.apply {
                navigationArgs.imageUri?.let { uriString ->
                    imgAddImage.setImageURI(uriString.toUri())
                    tvAddImageName.text = getFileNameFromUri(uriString.toUri(), requireActivity())
                }
                btnAddSave.setOnClickListener {
                    if (!isTextFieldEmpty()) {
                        isUsed = true
                        addWarranty()
                    }
                }
                btnAddTakePicture.setOnClickListener {
                    navigationArgs.imageUri?.let { uriString ->
                        //Delete old image
                        deleteFileByUri(uriString.toUri(), requireActivity())
                    }
                    findNavController().navigate(R.id.action_addWarrantyFragment_to_cameraFragment)
                }
            }
        }

        binding.apply {
            btnAddCancel.setOnClickListener { findNavController().navigateUp() }

            tilEtAddExpirationDate.setOnClickListener {
                val dateConstraints = CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.now())
                    .build()
                val datePicker =
                    MaterialDatePicker.Builder.datePicker()
                        .setCalendarConstraints(dateConstraints)
                        .setTitleText("Select date")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build()
                datePicker.apply {
                    addOnPositiveButtonClickListener { date ->
                        expirationDateInMillis = date
                        binding.tilEtAddExpirationDate.setText(formatDateMillis(expirationDateInMillis))
                    }
                    show(this@AddWarrantyFragment.requireActivity().supportFragmentManager, "tag")
                }
            }
        }
    }

    private fun bind(warranty: Warranty) {
        binding.apply {
            tilEtAddWarrantyName.setText(warranty.warrantyName)
            expirationDateInMillis = warranty.expirationDate
            tilEtAddExpirationDate.setText(formatDateMillis(expirationDateInMillis))
            if (navigationArgs.imageUri != null) {
                val uri = navigationArgs.imageUri!!.toUri()
                imgAddImage.setImageURI(uri)
                tvAddImageName.text = getFileNameFromUri(uri, requireActivity())
            } else {
                warranty.imageUri?.let {
                    imgAddImage.setImageURI(it.toUri())
                    tvAddImageName.text = getFileNameFromUri(it.toUri(), requireActivity())
                }
            }
        }
    }

    private fun addWarranty() {
        sharedViewModel.addNewWarranty(
            warrantyName = binding.tilEtAddWarrantyName.text.toString(),
            expirationDate = expirationDateInMillis,
            imageUri = navigationArgs.imageUri
        )
        findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
    }

    private fun updateWarranty() {
        //Delete old image if user have taken a new image when clicking save and if the warranty have an old image
        if (navigationArgs.imageUri != null && warranty.imageUri != null) {
            deleteFileByUri(warranty.imageUri!!.toUri(), requireActivity())
        }
        val updatedWarranty = warranty.copy(
            warrantyName = binding.tilEtAddWarrantyName.text.toString(),
            expirationDate = expirationDateInMillis,
            imageUri = navigationArgs.imageUri ?: warranty.imageUri
        )
        sharedViewModel.updateWarranty(updatedWarranty)
        findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
    }

    private fun deleteWarranty() {
        sharedViewModel.deleteWarranty(warranty)
        findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(android.R.string.dialog_alert_title))
            .setMessage(getString(R.string.delete_message_text))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                deleteWarranty()
                deleteFileByUri(warranty.imageUri!!.toUri(), requireActivity())
            }
            .show()
    }

    private fun isTextFieldEmpty(): Boolean {
        binding.apply {
            return if (!tilEtAddWarrantyName.text.isNullOrBlank() && expirationDateInMillis > 0) {
                tilAddWarrantyName.error = null
                tilAddExpirationDate.error = null
                false
            } else {
                if (tilEtAddWarrantyName.text.isNullOrBlank()) {
                    tilAddWarrantyName.error = getString(R.string.warranty_name_error_text)
                }
                if (tilEtAddExpirationDate.text.isNullOrBlank()) {
                    tilAddExpirationDate.error = getString(R.string.expiration_date_error_text)
                }
                true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //Delete unsaved image taken when exiting fragment
        if (navigationArgs.imageUri != null && !isUsed) {
            deleteFileByUri(navigationArgs.imageUri!!.toUri(), requireActivity())
        }
        _binding = null
    }
}