package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neyhuansikoko.warrantylogger.R
import com.neyhuansikoko.warrantylogger.WarrantyLoggerApplication
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.databinding.FragmentAddWarrantyBinding
import com.neyhuansikoko.warrantylogger.formatDateMillis
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModelFactory

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

                    binding.btnAddSave.setOnClickListener {
                        if (!isTextFieldEmpty()) {
                            updateWarranty()
                        }
                    }

                    binding.btnAddDelete.setOnClickListener {
                        showConfirmationDialog()
                    }
                    binding.btnAddDelete.visibility = View.VISIBLE
                }
            }
        } else {
            binding.btnAddSave.setOnClickListener {
                if (!isTextFieldEmpty()) {
                    addWarranty()
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
            tilEtAddExpirationDate.setText(formatDateMillis(warranty.expirationDate))
        }
    }

    private fun addWarranty() {
        sharedViewModel.addNewWarranty(
            warrantyName = binding.tilEtAddWarrantyName.text.toString(),
            expirationDate = expirationDateInMillis,
            imageUri = null //TODO: Implement capturing image
        )
        findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
    }

    private fun updateWarranty() {
        val updatedWarranty = warranty.copy(
            warrantyName = binding.tilEtAddWarrantyName.text.toString(),
            expirationDate = expirationDateInMillis,
            imageUri = null //TODO: Implement capturing image
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
        _binding = null
    }
}