package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.neyhuansikoko.warrantylogger.DEFAULT_MODEL
import com.neyhuansikoko.warrantylogger.R
import com.neyhuansikoko.warrantylogger.WarrantyListAdapter
import com.neyhuansikoko.warrantylogger.databinding.FragmentWarrantyListBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel

class WarrantyListFragment : Fragment() {

    private var _binding: FragmentWarrantyListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val sharedViewModel: WarrantyViewModel by activityViewModels()

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
                val action = WarrantyListFragmentDirections.actionWarrantyListFragmentToAddWarrantyFragment(title = getString(R.string.add_warranty_title_text))
                findNavController().navigate(action)
            }

            val adapter = WarrantyListAdapter {
                sharedViewModel.displayModel.value = it //Get reference
                sharedViewModel.inputModel = it.copy() //Get value
                findNavController().navigate(R.id.action_warrantyListFragment_to_warrantyDetailFragment)
            }
            rvListWarranty.adapter = adapter
            rvListWarranty.addItemDecoration(MaterialDividerItemDecoration(requireContext(), MaterialDividerItemDecoration.VERTICAL))

            //Fade the FAB when the user are scrolling down and show it when scrolling up
            rvListWarranty.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        fabList.hide()
                    } else fabList.show()
                }
            })

            sharedViewModel.allWarranties.observe(viewLifecycleOwner) {
                adapter.submitList(it)
                progressList.visibility = View.GONE
            }

            //TODO: Remove
//            sharedViewModel.testInsertTwentyWarranty()
        }
    }

    override fun onResume() {
        sharedViewModel.displayModel.value = DEFAULT_MODEL
        sharedViewModel.inputModel = DEFAULT_MODEL
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}