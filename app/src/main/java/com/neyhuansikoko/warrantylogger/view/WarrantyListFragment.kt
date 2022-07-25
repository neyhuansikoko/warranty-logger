package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.neyhuansikoko.warrantylogger.R
import com.neyhuansikoko.warrantylogger.WarrantyListAdapter
import com.neyhuansikoko.warrantylogger.WarrantyLoggerApplication
import com.neyhuansikoko.warrantylogger.databinding.FragmentWarrantyListBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModelFactory

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class WarrantyListFragment : Fragment() {

    private var _binding: FragmentWarrantyListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val sharedViewModel: WarrantyViewModel by activityViewModels {
        WarrantyViewModelFactory((activity?.applicationContext as WarrantyLoggerApplication).database.warrantyDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentWarrantyListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            fabList.setOnClickListener {
                findNavController().navigate(R.id.action_warrantyListFragment_to_addWarrantyFragment)
            }

            val adapter = WarrantyListAdapter {
                val action = WarrantyListFragmentDirections.actionWarrantyListFragmentToWarrantyDetailFragment(it.id)
                findNavController().navigate(action)
            }
            rvListWarranty.adapter = adapter

            sharedViewModel.allWarranties.observe(viewLifecycleOwner) {
                adapter.submitList(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}