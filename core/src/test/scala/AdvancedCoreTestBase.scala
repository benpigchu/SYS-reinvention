import java.io._
import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class AdvancedCoreTesterBase(core:Core) extends CoreTesterBase(core){
	protected val serialOutput:StringBuilder=new StringBuilder
	protected override def getByte(address:Long):Short={
		if(address==0x80000004L){
			println("----serial-status---")
			return 0x0f
		}
		if(address==0x80000000L){
			println("----serial-output---")
			return 0x00
		}
		super.getByte(address)
	}
	protected override def setByte(address:Long,data:Short)={
		if(address==0x80000000L){
			val char=data.toChar
			println(f"----serial-input[ $char ( $data ) ]----")
			serialOutput+=char
		}
		super.setByte(address,data)
	}
	protected def loadImage(file:InputStream)={
		val bytes=file.readAllBytes()
		for((byte,id)<- bytes.zipWithIndex){
			setByte(id,byte)
		}
	}
}

class WithMonitorTester(core:Core) extends AdvancedCoreTesterBase(core){
	loadImage(getClass.getResourceAsStream("/monitor.bin"))
	println(s"${getWord(0)}-${getWord(4)}-${getWord(8)}-${getWord(12)}")
	stepStages(200)
	println(s"output:size=${serialOutput.size}")
	println(serialOutput.result)
}

class WithMonitorTest extends CoreTestBase(c=>new WithMonitorTester(c),false){}