import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class FenceCoreTester(core:Core) extends CoreTesterBase(core){
	import InstGen._
	val add31=ADDI(31,0,8)
	val insts=Seq(
		FENCE(0xF,0xF),//do nothing//0
		LUI(1,add31&0xFFFFF000L),
		LUI(2,0x1000),
		ADDI(2,2,0xFFFFFFFF),
		ANDI(2,2,add31),//10
		ADD(1,1,2),
		SW(0,1,0x20),
		FENCE_I,
		NOP,//20//put add31 into this
		NOP,
		NOP,
		NOP,
		NOP//30
	)
	loadInst(insts)
	stepStages(15)
	expect(core.io.debug.regs(1),add31)
	expect(core.io.debug.regs(31),8)
}

class FenceCoreTest extends CoreTestBase(c=>new FenceCoreTester(c)){}