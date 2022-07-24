package com.neyhuansikoko.warrantylogger

import android.app.ProgressDialog.show
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.google.android.material.datepicker.MaterialDatePicker
import com.neyhuansikoko.warrantylogger.databinding.FragmentAddWarrantyBinding
import java.text.SimpleDateFormat

class AddWarrantyFragment : Fragment() {

    private var _binding: FragmentAddWarrantyBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
            tilEtAddWarrantyName.setOnClickListener {
                val datePicker =
                    MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build()
                datePicker.apply {
                    addOnPositiveButtonClickListener { date ->
                        binding.tilEtAddWarrantyName.setText(SimpleDateFormat("dd/MM/yyyy").format(date))
                    }
                    show(this@AddWarrantyFragment.requireActivity().supportFragmentManager, "tag")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}