/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.fragment

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.kroegerama.kaiteki.bcode.BarcodeResultListener
import com.kroegerama.kaiteki.bcode.ui.BarcodeBottomSheet
import com.wireguard.android.R
import com.wireguard.android.activity.TunnelCreatorActivity
import com.wireguard.android.util.ImportEventsListener
import com.wireguard.android.util.resolveAttribute
import com.google.android.material.R as materialR

class AddTunnelsSheet() : BottomSheetDialogFragment(), BarcodeResultListener {

    private var fragmentListener: ImportEventsListener? = null

    constructor(listener: ImportEventsListener) : this() {
        fragmentListener = listener
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (savedInstanceState != null) dismiss()
        return inflater.inflate(R.layout.add_tunnels_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dialog = dialog as BottomSheetDialog? ?: return
                val bottomSheet = dialog.findViewById<FrameLayout>(materialR.id.design_bottom_sheet)
                        ?: return
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            dismiss()
                        }
                    }
                }
                dialog.findViewById<MaterialButton>(R.id.create_empty)?.setOnClickListener {
                    dismiss()
                    onRequestCreateConfig()
                }
                dialog.findViewById<MaterialButton>(R.id.create_from_file)?.setOnClickListener {
                    dismiss()
                    onRequestImportConfig()
                }
                dialog.findViewById<MaterialButton>(R.id.create_from_qrcode)?.setOnClickListener {
                    dismiss()
                    onRequestScanQRCode()
                }
            }
        })
        val gradientDrawable = GradientDrawable().apply {
            setColor(ctx.resolveAttribute(R.attr.colorBackground))
        }
        view.background = gradientDrawable
    }

    override fun onBarcodeResult(result: Result): Boolean {
        fragmentListener?.onQrImport(result.text)
        return true
    }

    override fun onBarcodeScanCancelled() {
    }

    private fun onRequestCreateConfig() {
        startActivity(Intent(activity, TunnelCreatorActivity::class.java))
    }

    private fun onRequestImportConfig() {
        fragmentListener?.onRequestImport()
    }

    private fun onRequestScanQRCode() {
        BarcodeBottomSheet.show(
                childFragmentManager,
                formats = listOf(BarcodeFormat.QR_CODE),
                barcodeInverted = false
        )
    }
}
