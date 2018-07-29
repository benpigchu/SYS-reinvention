import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class ForwardCoreTester(core:Core) extends CoreTesterBase(core){
	import InstGen._
	val insts=Seq(
		LUI(1,0x100000),
		ADDI(1,1,0x80),//exe->exe
		LW(15,1,0),
		NOP,
		ADDI(15,15,0xF),//load->nop->exe
		LW(23,1,0x10),
		ADDI(23,23,0xE),//load->exe
		NOP,
		NOP,
		NOP,
		NOP
	)
	loadInst(insts)
	setWord(0x100080L,0xDEADBEE0L)
	setWord(0x100090L,0xCAFEBAB0L)
	stepStages(15)
	expect(core.io.debug.regs(1),0x100080L)
	expect(core.io.debug.regs(15),0xDEADBEEFL)
	expect(core.io.debug.regs(23),0xCAFEBABEL)
}

class ForwardCoreTest extends CoreTestBase(c=>new ForwardCoreTester(c)){}