package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import android.view.*
import android.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.neyhuansikoko.warrantylogger.R
import com.neyhuansikoko.warrantylogger.WarrantyListAdapter
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.databinding.FragmentWarrantyListBinding
import com.neyhuansikoko.warrantylogger.viewmodel.WarrantyViewModel
import com.neyhuansikoko.warrantylogger.viewmodel.notifyObserver

class WarrantyListFragment : Fragment() {

    private var _binding: FragmentWarrantyListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val adapter by lazy { binding.rvListWarranty.adapter as WarrantyListAdapter }

    private val sharedViewModel: WarrantyViewModel by activityViewModels()

    private val clickListener: (Warranty) -> Unit = {
        navigateToDetail(it)
    }

    private val contextListener: (Warranty, Boolean) -> Unit = { warranty, isChecked ->
        if (isChecked) {
            sharedViewModel.addDelete(warranty)
        } else {
            binding.cbListDeleteAll.isChecked = false
            adapter.selectAll = false

            sharedViewModel.removeDelete(warranty)
        }
    }

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater: MenuInflater = mode.menuInflater
            inflater.inflate(R.menu.context_menu_list, menu)
            return true
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val size = sharedViewModel.deleteSize
            actionMode?.title = "Selected $size ${if (size > 1) "items" else "item"}"
            return true
        }

        // Called when the user selects a contextual menu item
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.menu_item_delete -> {
                    showConfirmationDialog() // Action picked, so close the CAB
                    true
                }
                else -> false
            }
        }

        // Called when the user exits the action mode
        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            sharedViewModel.clearDelete()
        }
    }

    private var actionMode: ActionMode? = null

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

            val adapter = WarrantyListAdapter(clickListener, contextListener)

            cbListDeleteAll.setOnClickListener {
                adapter.apply {
                    if (selectAll) {
                        adapter.setUnselectAll()
                        actionMode?.finish()
                    } else {
                        setSelectAll()
                        sharedViewModel.addAllToDelete()
                    }
                }
            }

            rvListWarranty.apply {
                this.adapter = adapter
                this.addItemDecoration(MaterialDividerItemDecoration(requireContext(), MaterialDividerItemDecoration.VERTICAL))

                //Fade the FAB when the user are scrolling down and show it when scrolling up
                this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (dy > 0) {
                            fabList.hide()
                        } else fabList.show()
                    }
                })
            }

            sharedViewModel.apply {
                allWarranties.observe(viewLifecycleOwner) {
                    adapter.submitList(it)
                    progressList.visibility = View.GONE
                    cbListDeleteAll.visibility = if (it.isNotEmpty()) View.VISIBLE else View.INVISIBLE
                    tvListNoData.visibility = if (it.isEmpty()) View.VISIBLE else View.INVISIBLE
                }

                deleteList.observe(viewLifecycleOwner) {
                    if (it.isEmpty()) {
                        cbListDeleteAll.isChecked = false
                        actionMode?.finish()
                        binding.fabList.show()
                    }
                    else if (actionMode == null) {
                        binding.fabList.hide()
                        actionMode = activity?.startActionMode(actionModeCallback)
                    }

                    if (deleteSize == warrantySize && warrantySize > 0) {
                        cbListDeleteAll.isChecked = true
                        adapter.selectAll = true
                    }

                    actionMode?.invalidate()
                }
            }

            //TODO: Remove
//            sharedViewModel.testInsertTwentyWarranty()
        }
    }

    private fun navigateToDetail(warranty: Warranty) {
        if (actionMode == null) {
            sharedViewModel.assignModel(warranty)
            findNavController().navigate(R.id.action_warrantyListFragment_to_warrantyDetailFragment)
        }
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(android.R.string.dialog_alert_title))
            .setMessage(getString(R.string.delete_message_text))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                sharedViewModel.deleteSelectedWarranty()
                actionMode?.finish()
            }
            .show()
    }

    override fun onResume() {
        sharedViewModel.apply {
            resetModel()
            clearTempImage()
        }

        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedViewModel.clearDelete()
        _binding = null
    }
}