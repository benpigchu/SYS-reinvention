import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class MemCoreTester(core:Core) extends CoreTesterBase(core){
	import InstGen._
	val insts=Seq(
		LUI(1,0x100000),
		LUI(16,0xCAFEC000),
		NOP,
		NOP,
		NOP,
		NOP,
		LW(2,1,0x80),
		LH(3,1,0x80),
		LB(4,1,0x80),
		LHU(5,1,0x80),
		LBU(6,1,0x80),
		ADDI(16,16,0xABE),
		NOP,
		NOP,
		NOP,
		NOP,
		SW(1,16,0x80),
		SH(1,16,0x82),
		SB(1,16,0x83),
		LW(17,1,0x80),
		NOP,
		NOP,
		NOP,
		NOP
	)
	loadInst(insts)
	setWord(0x100080L,0xDEADBEEFL)
	stepStages(24)
	expect(core.io.debug.regs(2),0xDEADBEEFL)
	expect(core.io.debug.regs(3),0xFFFFBEEFL)
	expect(core.io.debug.regs(4),0xFFFFFFEFL)
	expect(core.io.debug.regs(5),0x0000BEEFL)
	expect(core.io.debug.regs(6),0x000000EFL)
	expect(core.io.debug.regs(16),0xCAFEBABEL)
	expect(core.io.debug.regs(17),0xBEBEBABEL)
}

class MemCoreTest extends CoreTestBase(c=>new MemCoreTester(c)){}