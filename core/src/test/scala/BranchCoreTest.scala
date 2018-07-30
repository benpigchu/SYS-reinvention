import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class BranchCoreTester(core:Core) extends CoreTesterBase(core){
	import InstGen._
	val insts=Seq(
		ADDI(1,0,0x28),//0
		BEQ(1,1,16),//jump to bge
		XORI(2,0,8),
		JAL(31,16),//jump to ori
		ADDI(3,0,8),//10
		BGE(1,0,0xFFFFFFF4),//jump to xori
		ADDI(4,0,8),
		ORI(5,0,0xF),
		JALR(15,1,0),//jump to slti//20
		ADDI(6,0,8),
		SLTI(7,0,8),
		NOP,
		NOP,//30
		NOP,
		NOP
	)
	loadInst(insts)
	stepStages(18)
	expect(core.io.debug.regs(2),8)
	expect(core.io.debug.regs(3),0)
	expect(core.io.debug.regs(4),0)
	expect(core.io.debug.regs(5),0xF)
	expect(core.io.debug.regs(6),0)
	expect(core.io.debug.regs(7),1)
	expect(core.io.debug.regs(15),0x24)
	expect(core.io.debug.regs(31),0x10)
}

class BranchCoreTest extends CoreTestBase(c=>new BranchCoreTester(c)){}