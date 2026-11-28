package fr.luba.poc.camera

import fr.luba.poc.gateway.KeepaliveLoss
import fr.luba.poc.proto.LubaMsgProto.LubaMsg
import fr.luba.poc.proto.LubaMsgProto.MsgCmdType
import fr.luba.poc.proto.LubaMsgProto.MsgDevice
import fr.luba.poc.proto.LubaMulProto.Getlamprsp
import fr.luba.poc.proto.LubaMulProto.SetHeadlamp
import fr.luba.poc.proto.LubaMulProto.Setlamprsp
import fr.luba.poc.proto.LubaMulProto.SocMul
import fr.luba.poc.proto.LubaMulProto.lamp_ctrl_sta
import fr.luba.poc.proto.LubaMulProto.lamp_manual_ctrl_sta
import fr.luba.poc.proto.MctrlSysProto.MctlSys
import fr.luba.poc.proto.MctrlSysProto.report_info_data
import fr.luba.poc.proto.MctrlSysProto.rpt_dev_status
import fr.luba.poc.protocol.Commands
import fr.luba.poc.protocol.ReadOnlyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Headlamp (Yuka Mini request, 2026-11-28): PyMammotion `set_car_manual_light` / `get_car_light`. */
class HeadlampTest {
    @Test fun switchMatchesPyMammotionSetCarManualLight() {
        val on = LubaMsg.parseFrom(Commands().headlamp(true))
        assertEquals(MsgCmdType.MSG_CMD_TYPE_MUL, on.msgtype)
        assertEquals(MsgDevice.SOC_MODULE_MULTIMEDIA, on.rcver)
        assertEquals(1125, on.mul.setLamp.setIds)
        assertEquals(2, on.mul.setLamp.lampPowerCtrl)
        assertEquals(lamp_manual_ctrl_sta.manual_power_on, on.mul.setLamp.lampManualCtrl)
        val off = LubaMsg.parseFrom(Commands().headlamp(false)).mul.setLamp
        assertEquals(1127, off.setIds)
        assertEquals(lamp_manual_ctrl_sta.manual_power_off, off.lampManualCtrl)
        assertEquals(1126, LubaMsg.parseFrom(Commands().headlampRead()).mul.getLamp.getIds)
    }

    @Test fun readOnlyPolicyAcceptsOnlyTheManualSwitch() {
        assertTrue(ReadOnlyPolicy.allows(Commands().headlamp(true)))
        assertTrue(ReadOnlyPolicy.allows(Commands().headlamp(false)))
        assertTrue(ReadOnlyPolicy.allows(Commands().headlampRead()))
        // Automatic night mode (1121) and brightness stay refused.
        val night = LubaMsg.newBuilder().setMsgtype(MsgCmdType.MSG_CMD_TYPE_MUL).setRcver(MsgDevice.SOC_MODULE_MULTIMEDIA)
            .setMul(SocMul.newBuilder().setSetLamp(SetHeadlamp.newBuilder().setSetIds(1121).setLampPowerCtrl(1).setLampCtrl(lamp_ctrl_sta.power_ctrl_on))).build()
        assertFalse(ReadOnlyPolicy.allows(night.toByteArray()))
    }

    @Test fun headlampWritesDoNotStopAMissionThroughTheGateway() {
        for (label in listOf("HEADLAMP_ON", "HEADLAMP_OFF", "HEADLAMP_READ")) assertTrue(label, KeepaliveLoss.keepalive(false, label))
    }

    @Test fun stateFollowsTheRobotAnswers() {
        val observer = CameraRobotObserver()
        assertNull(observer.value.headlampOn)
        fun mul(b: SocMul.Builder) = LubaMsg.newBuilder().setMul(b).build()
        assertTrue(observer.accept(mul(SocMul.newBuilder().setGetLampRsp(Getlamprsp.newBuilder().setGetIds(1126).setLampManualCtrl(lamp_manual_ctrl_sta.manual_power_on))), 0))
        assertEquals(true, observer.value.headlampOn)
        observer.accept(mul(SocMul.newBuilder().setSetLampRsp(Setlamprsp.newBuilder().setSetIds(1127).setResult(0))), 0)
        assertEquals(false, observer.value.headlampOn)
        assertEquals(0, observer.value.headlampResult)
        val report = LubaMsg.newBuilder().setSys(MctlSys.newBuilder().setToappReportData(report_info_data.newBuilder()
            .setDev(rpt_dev_status.newBuilder().setHeadlampStatus(1)))).build()
        observer.accept(report, 0)
        assertEquals(1, observer.value.headlampStatus)
    }
}
