package de.troido.bleacon.scanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import de.troido.bleacon.ble.HandledBleActor
import de.troido.bleacon.ble.obtainScanner
import de.troido.bleacon.config.BleFilter
import java.util.UUID

/**
 * A more idiomatic wrapper for [android.bluetooth.le.BluetoothLeScanner].
 *
 * @param[svcUuid] UUID of the target service.
 *
 * @param[chrUuid] UUID of the target characteristic of the target service.
 *
 * @param[autoConnect] if `false`, directly connect to the remote device, or if `true`,
 * automatically connect as soon as the remote device becomes available.
 * See [android.bluetooth.BluetoothDevice.connectGatt]'s `autoConnect` param for more details.
 *
 * @param[stopWhenFound] if `true` then the scanner will be automatically stopped on first result.
 *
 * @param[handler] optional handler for sharing with other asynchronous actions.
 */
class BleScanner(context: Context,
                 filter: BleFilter,
                 svcUuid: UUID,
                 chrUuid: UUID,
                 callback: BleScanCallback,
                 settings: BleScanSettings = BleScanSettings(),
                 autoConnect: Boolean = false,
                 stopWhenFound: Boolean = true,
                 handler: Handler = Handler()
) : HandledBleActor() {

    private val scanner = obtainScanner()

    private val filters = listOf(filter.filter)
    private val scanSettings = settings.settings

    private val gattCallback = callback.toBtGattCallback(svcUuid, chrUuid, this)

    private val scanCallback = object : ScanCallback() {
        private fun connectDevice(device: BluetoothDevice) {
            if (stopWhenFound) {
                scanner.stopScan(this)
            }

            device.connectGatt(context, autoConnect, gattCallback)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let(this::connectDevice)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.mapNotNull { it.device }?.firstOrNull()?.let(this::connectDevice)
        }
    }

    override fun start() {
        handler.post { scanner.startScan(filters, scanSettings, scanCallback) }
    }

    override fun stop() {
        handler.post { scanner.stopScan(null) }
    }
}
