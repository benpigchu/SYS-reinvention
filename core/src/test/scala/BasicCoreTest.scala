import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

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
	stepStages(12)
	expect(core.io.debug.regs(31),3)
	expect(core.io.debug.regs(15),1L<<12)
	expect(core.io.debug.regs(7),6)
	expect(core.io.debug.regs(5),(2L<<12)+4)
}

class BasicCoreTest extends CoreTestBase(c=>new BasicCoreTester(c)){}