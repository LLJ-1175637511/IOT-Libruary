package com.llj.baselib.ui

import android.app.Activity
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
        //检查权限
        checkPermission()
        //检查用户名密码
        loadUserData()
    }

    private fun checkPermission() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                initPermission(),
                REQUEST_CODE_PERMISSIONS
            )
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

    fun loadUserData() = IOTLib.loadUserData()

    fun <T : Activity> login(username: String, password: String, target: Class<T>) {
        if (username.isEmpty()) {
            ToastUtils.toastShort("用户名不能为空")
            return
        }
        if (password.isEmpty()) {
            ToastUtils.toastShort("密码不能为空")
            return
        }
        requestAndSaveToken {
            IOTLib.saveUserInfo(username,password)
            startActivityAndFinish(target)
        }
    }

    fun requestAndSaveToken(block: () -> Unit) {
        kotlin.runCatching {
            lifecycleScope.launch(Dispatchers.IO) {
                val tokenBean = IOTRepository.requestToken()
                val token = tokenBean.access_token
                runOnUiThread {
                    if (token.isNotEmpty()) {
                        IOTLib.savedToken(token)
                        block()
                    } else {
                        ToastUtils.toastShort("token信息获取失败")
                    }
                }

            }
        }
    }

    fun login(username: String, password: String, block: () -> Unit) {
        if (username.isEmpty()) {
            ToastUtils.toastShort("用户名不能为空")
            return
        }
        if (password.isEmpty()) {
            ToastUtils.toastShort("密码不能为空")
            return
        }
        IOTLib.saveUserInfo(username, password)
        block()
    }

}