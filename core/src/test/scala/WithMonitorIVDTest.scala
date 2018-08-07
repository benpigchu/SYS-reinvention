import java.io._
import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class WithMonitorIVDTester(core:Core) extends AdvancedCoreTesterBase(core){
	loadImage(getClass.getResourceAsStream("/monitor.bin"))
	serialInput++="I 0x200000\nlui 0x1f 0xcafe000\njalr 0x0 0x1 0x0\n\nV 0x200000 0x2\nD 0x200000 0x2\n"
	//lui x31 0xcafe
	//jalr ra(0)
	stepStages(40000)
	println(s"output:size=${serialOutput.size}")
	println(serialOutput.result)
	expect(serialInput.size==0,"consume all input")
	expect(("Welcome to System on Cat!\n"
		++"Monitor v0.1\n"
		++"Build specs:\n"
		++"  WITH_CSR = off\n"
		++"  WITH_INTERRUPT = off\n"
		++"  WITH_IRQ = off\n"
		++"  WITH_ECALL = off\n"
		++">>> [0x00200000] >>> [0x00200000] [0x00200004] [0x00200008] >>> [0x00200000] 0x0cafefb7\n"
		++"[0x00200004] 0x00008067"
		++">>> [0x00200000] lui 0x0000001f 0x0cafe000"
		++"[0x00200004] jalr 0x00000000 0x00000001 0x00000000"
		++">>> ")==serialOutput.result,"have current output")
}

class WithMonitorIVDTest extends CoreTestBase(c=>new WithMonitorIVDTester(c),false){}