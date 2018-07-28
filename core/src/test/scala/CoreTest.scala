import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class CoreTesterBase(core:Core)extends PeekPokeTester(core){
	//we use only the lower half bits, why we can not have unsigned number in jvm?
	protected val mem=ArrayBuffer.fill[Short](0x800000)(0)
	//init mem
	protected def getByte(address:Long):Short={
		val addr=address%0x3FFFFFFFFL
		if(address<0||address>mem.length){
			return 0
		}
		mem(address)
	}
	protected def setByte(address:Long,data:Short)={
		val addr=address%0x3FFFFFFFFL
		if(!(address<0||address>mem.length)){
			mem(address)=data
		}
	}
	protected def getHalf(address:Long):Int={
		(getByte(address+1).intValue<<8)+getByte(address).intValue
	}
	protected def setHalf(address:Long,data:Int)={
		setByte(address,(data&0xFF).shortValue)
		setByte(address+1,((data>>8)&0xFF).shortValue)
	}
	protected def getWord(address:Long):Long={
		(getHalf(address+2).longValue<<16)+getHalf(address).longValue
	}
	protected def setWord(address:Long,data:Long)={
		setHalf(address,(data&0xFFFF).intValue)
		setHalf(address+2,((data>>16)&0xFFFF).intValue)
	}
	protected def loadInst(insts:Iterable[Long])={
		for((inst,id) <- insts.zipWithIndex){
			setWord(id*4,inst)
		}
	}
	protected def stepStage()={
		poke(core.io.mem.ready,false)
		val iaddr=peek(core.io.mem.iaddr).longValue
		poke(core.io.mem.idata,getWord(iaddr))
		poke(core.io.mem.ready,true)
		step(1)
	}
}

class BasicCoreTester(core:Core) extends CoreTesterBase(core){
	import InstGen._
	val insts=Seq(
		ADDI(31,0,3),
		AUIPC(5,2L<<12),
		LUI(15,1L<<12),
		NOP,
		NOP,
		NOP,
		NOP,
		ADD(7,31,31),
		NOP,
		NOP,
		NOP,
		NOP
	)
	loadInst(insts)
	for(i<-0 until 12){
		stepStage()
	}
	expect(core.io.debug.regs(31),3)
	expect(core.io.debug.regs(15),1L<<12)
	expect(core.io.debug.regs(7),6)
	expect(core.io.debug.regs(5),(2L<<12)+4)
}

class BasicCoreTest extends ChiselFlatSpec{
	"Core" should "works" in {
		iotesters.Driver.execute(Array("--is-verbose"),()=>new Core){
			c=>new CoreTester(c)
		}should be(true)
	}
}