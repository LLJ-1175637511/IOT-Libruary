package com.llj.baselib.ui

import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.llj.baselib.IOTLib
import com.llj.baselib.bean.Const
import com.llj.baselib.net.IOTRepository
import com.llj.baselib.save
import com.llj.baselib.utils.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class IOTLoginActivity<DB : ViewDataBinding> : IOTBaseActivity<DB>() {

    override fun init() {
        super.init()
        checkPermission()
    }

    private fun checkPermission() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                initPermission(),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            updateBigIotToken()
        }
    }

    private fun updateBigIotToken() {
        kotlin.runCatching {
            lifecycleScope.launch(Dispatchers.IO) {
                val tokenBean = IOTRepository.requestToken()
                val token = tokenBean.access_token
                if (token.isNotEmpty()) {
                    IOTLib.getSP(Const.SPNet).save {
                        putString(Const.SPToken,token)
                    }
                } else {
                    ToastUtils.toastShort("token信息获取失败")
                }
            }
        }
    }

    abstract fun initPermission(): Array<String>

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                ToastUtils.toastShort("权限未允许")
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = initPermission().all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

}