package com.llj.baselib.test

import android.os.Bundle
import androidx.activity.viewModels
import com.llj.baselib.IOTActivity
import com.llj.baselib.vm.IOTViewModel

class TestActivity:IOTActivity() {

    private val vm by viewModels<TestVM>()

    override fun webState(state: IOTViewModel.WebSocketType) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm.connect(this)

    }

}