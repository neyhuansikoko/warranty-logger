package com.neyhuansikoko.warrantylogger.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.databinding.FragmentBackupBinding
import com.neyhuansikoko.warrantylogger.viewmodel.BackupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: BackupViewModel by activityViewModels()

    private var snackbar: Snackbar? = null

    private val backupChooserActivity = registerForActivityResult(ActivityResultContracts.GetContent()) { backupUri ->
        //Return content URI
        if (backupUri != null) {
            context?.contentResolver?.let { contentResolver ->
                if (contentResolver.getType(backupUri).equals(ZIP_CONTENT_TYPE)) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val internalBackup = File(requireActivity().applicationContext.filesDir, BACKUP_ZIP)

                        context?.contentResolver?.openInputStream(backupUri).use { inputStream ->
                            internalBackup.outputStream().use { outputStream ->
                                inputStream?.copyTo(outputStream)
                            }
                        }

                        viewModel.onBackupImported()
                        displayShortMessage(
                            binding.tvBackupImport,
                            "Backup import is complete"
                        )
                    }
                } else {
                    displayLongMessage(
                        binding.tvBackupImport,
                        "Incompatible file format. Please choose a ZIP file"
                    )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            //Backup button click listener
            layoutBackupBackup.setOnClickListener {
                onBackup()
            }
            //Restore button click listener
            tvBackupRestore.setOnClickListener {
                showConfirmationDialog()
            }

            //Export backup to download folder
            tvBackupExport.setOnClickListener {
                onExport()
            }

            tvBackupImport.setOnClickListener {
                onImport()
            }

            viewModel.lastBackupDate.observe(viewLifecycleOwner) { date ->
                tvBackupDate.text = formatBackupDate(date)

                if (date != null) {
                    tvBackupExport.visibility = View.VISIBLE
                } else {
                    tvBackupExport.visibility = View.GONE
                }
            }
            viewModel.backupComplete.observe(viewLifecycleOwner) { completed ->
                snackbar?.dismiss()
                snackbar = if (completed) {
                    displayShortMessage(
                        binding.tvBackupBackup,
                        "Backup is completed",
                    )
                } else {
                    displayShortMessage(
                        binding.tvBackupBackup,
                        "Backup has failed",
                    )
                }
            }
        }
    }

    private fun onImport() {
        backupChooserActivity.launch("application/*")
    }

    private fun onExport() {
        viewModel.exportBackupFile { path ->
            displayLongMessage(
                binding.tvBackupExport,
                "Exported to: $path"
            )
        }
    }

    private fun formatBackupDate(backupDate: String?): String {
        return if (backupDate != null) {
            "Last backup on: $backupDate"
        } else {
            getString(R.string.no_backup_set_text)
        }
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(android.R.string.dialog_alert_title))
            .setMessage(getString(R.string.restart_dialog_text))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                onRestore()
            }
            .show()
    }

    private fun onBackup() {
        snackbar = Snackbar.make(
            binding.tvBackupBackup,
            "Backup in progress..",
            Snackbar.LENGTH_SHORT
        ).also { it.show() }
        viewModel.backupDatabase()
    }

    private fun onRestore() {
        viewModel.restoreDatabase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}