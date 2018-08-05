import java.io._
import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class AdvancedCoreTesterBase(core:Core) extends CoreTesterBase(core){
	protected val serialOutput:StringBuilder=new StringBuilder
	protected val serialInput:StringBuilder=new StringBuilder
	protected override def getByte(address:Long):Short={
		if(address==0x80000004L){
			println("----serial-status---")
			return if(serialInput.size>0){0xFF}else{0x0F}
		}
		if(address==0x80000000L){
			println("----serial-input---")
			val char=serialInput(0)
			serialInput.deleteCharAt(0)
			return (char.toInt&0xFF).shortValue
		}
		super.getByte(address)
	}
	protected override def setByte(address:Long,data:Short)={
		if(address==0x80000000L){
			val char=data.toChar
			println(f"----serial-output[ $char ( $data ) ]----")
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

