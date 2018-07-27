import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class CoreTester(core:Core)extends PeekPokeTester(core){
	//we use only the lower half bits, why we can not have unsigned number in jvm?
	val mem=ArrayBuffer.fill[Short](0x800000)(0)
	//init mem
	private def getByte(address:Long):Short={
		val addr=address%0x3FFFFFFFFL
		if(address<0||address>mem.length){
			return 0
		}
		mem(address)
	}
	private def setByte(address:Long,data:Short)={
		val addr=address%0x3FFFFFFFFL
		if(!(address<0||address>mem.length)){
			mem(address)=data
		}
	}
	private def getHalf(address:Long):Int={
		(getByte(address+1).intValue<<8)+getByte(address).intValue
	}
	private def setHalf(address:Long,data:Int)={
		setByte(address,(data&0xFF).shortValue)
		setByte(address+1,((data>>8)&0xFF).shortValue)
	}
	private def getWord(address:Long):Long={
		(getHalf(address+2).longValue<<16)+getHalf(address).longValue
	}
	private def setWord(address:Long,data:Long)={
		setHalf(address,(data&0xFFFF).intValue)
		setHalf(address+2,((data>>16)&0xFFFF).intValue)
	}
	setWord(0,java.lang.Long.parseLong("00000000001100000000111110010011",2))
	for(i<-1 until 10){
		setWord(4*i,java.lang.Long.parseLong("00000000000000000000000000010011",2))
	}
	for(i<-0 until 5){
		poke(core.io.mem.ready,false)
		val iaddr=peek(core.io.mem.iaddr).longValue
		poke(core.io.mem.idata,getWord(iaddr))//NOP
		poke(core.io.mem.ready,true)
		step(1)
	}
	expect(core.io.debug.regs(31),3)
}

class CoreTest extends ChiselFlatSpec{
	"Core" should "works" in {
		iotesters.Driver.execute(Array("--is-verbose"),()=>new Core){
			c=>new CoreTester(c)
		}should be(true)
	}
}