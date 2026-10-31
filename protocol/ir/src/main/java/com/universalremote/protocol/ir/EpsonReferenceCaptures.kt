package com.universalremote.protocol.ir

import com.universalremote.core.model.IrSignal
import com.universalremote.core.model.RemoteCommand

/** Raw Pronto-converted G5650W captures, not a decoder or proof of EB-L260F compatibility.
 * Source: Flipper-IRDB/_Converted_/Pronto/E/Epson/Power_Lite_Pro_G5650W.ir.
 * Preserve both marks AND spaces: reducing each pair to one bit loses information.
 */
internal object EpsonReferenceCaptures {
    private fun capture(data: String): IrSignal {
        val frame = data.split(' ').map(String::toInt)
        // Source includes the trailing gap; repeat the complete captured frame twice.
        return IrSignal(38_000, frame + frame.dropLast(1))
    }

    private val menu = capture("2293 573 573 573 1146 573 573 573 573 1146 573 573 1146 573 1146 573 573 573 573 573 573 573 573 573 573 573 573 573 573 76340")
    private val esc = capture("2293 573 573 573 1146 573 573 573 573 573 573 1146 573 573 573 573 573 573 573 1146 573 573 573 573 573 573 573 573 573 76913")
    private val hdmi = capture("2293 573 573 573 1146 573 573 573 573 1146 573 1146 1146 1146 1146 1146 573 1146 573 573 573 573 573 573 573 573 573 573 573 74255")
    private val computer = capture("2293 573 573 573 1146 573 573 573 573 1146 573 1146 573 573 573 1146 573 1146 573 573 573 573 573 573 573 573 573 573 573 75975")
    private val lanUsb = capture("2293 573 573 573 1146 573 573 573 573 1146 573 573 1146 573 573 1146 573 1146 1146 573 573 573 573 573 573 573 573 573 573 75193")
    private val auto = capture("2293 573 573 573 1146 573 573 573 573 1146 573 1146 573 1146 1146 573 1146 573 573 573 573 573 573 573 573 573 573 573 573 75193")
    private val aspect = capture("2293 573 573 573 1146 573 573 573 573 1146 573 573 573 573 573 573 1146 573 1146 573 573 573 573 573 573 573 573 573 573 76340")
    private val color = capture("2293 573 573 573 1146 573 573 573 573 1146 573 1146 1146 1146 573 573 573 573 1146 573 573 573 573 573 573 573 573 573 573 75402")

    val commands = mapOf(
        RemoteCommand.MENU to menu,
        RemoteCommand.SETTINGS_KEY to menu,
        RemoteCommand.BACK to esc,
        // Epson_4650.ir: Default and User1, including their captured repeat gap.
        RemoteCommand.DEFAULT to capture("2290 609 573 588 1151 586 574 587 572 587 573 1167 572 587 572 587 573 587 572 1167 572 586 574 586 573 586 573 585 574 77352"),
        RemoteCommand.USER to capture("2310 587 571 590 1150 589 570 589 570 1169 570 1170 569 1170 569 1169 570 590 569 589 570 589 570 589 571 589 571 590 569 76194"),
        RemoteCommand.HDMI to hdmi,
        RemoteCommand.COMPUTER to computer,
        RemoteCommand.LAN to lanUsb,
        RemoteCommand.USB to lanUsb,
        RemoteCommand.AUTO_SETUP to auto,
        RemoteCommand.ASPECT_RATIO to aspect,
        RemoteCommand.COLOR_MODE to color,
        RemoteCommand.NUM_4 to auto,
        RemoteCommand.NUM_5 to aspect,
        RemoteCommand.NUM_6 to color,
        RemoteCommand.NUM_7 to capture("2293 573 573 573 1146 573 573 573 573 1146 573 1146 1146 573 573 573 1146 573 1146 573 1146 573 573 573 573 573 573 573 573 74829"),
        RemoteCommand.NUM_8 to capture("2293 573 573 573 1146 573 573 573 573 1146 573 1146 573 1146 573 573 573 573 1146 573 1146 573 573 573 573 573 573 573 573 75193"),
        RemoteCommand.NUM_9 to capture("2293 573 573 573 1146 573 573 573 573 1146 573 573 573 573 573 573 573 573 1146 573 1146 573 573 573 573 573 573 573 573 76340"),
    )
}
