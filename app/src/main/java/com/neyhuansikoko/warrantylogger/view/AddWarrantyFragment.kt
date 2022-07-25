package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.neyhuansikoko.warrantylogger.R
import com.neyhuansikoko.warrantylogger.WarrantyLoggerApplication
import com.neyhuansikoko.warrantylogger.databinding.FragmentAddWarrantyBinding
import com.neyhuansikoko.warrantylogger.formatDateMillis
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModelFactory
import java.text.SimpleDateFormat

class AddWarrantyFragment : Fragment() {

    private var _binding: FragmentAddWarrantyBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
        binding.apply {
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
            btnAddSave.setOnClickListener {
                if (!isTextFieldEmpty()) {
                    addWarranty()
                }
            }
        }
    }

    private fun addWarranty() {
        binding.apply {
            sharedViewModel.addNewWarranty(
                warrantyName = tilEtAddWarrantyName.text.toString(),
                expirationDate = expirationDateInMillis,
                imageUri = null //TODO: Implement capturing image
            )
        }
        findNavController().navigate(R.id.action_addWarrantyFragment_to_warrantyListFragment)
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