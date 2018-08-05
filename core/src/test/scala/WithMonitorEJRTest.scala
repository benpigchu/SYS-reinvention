import java.io._
import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class WithMonitorEJRTester(core:Core) extends AdvancedCoreTesterBase(core){
	loadImage(getClass.getResourceAsStream("/monitor.bin"))
	serialInput++="E 0x400000\n0x0cafefb7\n0x00008067\n\nJ 0x400000\nR\n"
	//lui x31 0xcafe
	//jalr ra(0)
	stepStages(80000)
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
		++">>> [0x00400000] [0x00400004] [0x00400008] >>> >>> x(0x00000000)  =  0x00000000\n"
		++"x(0x00000001)  =  0x00001e94\n"
		++"x(0x00000002)  =  0x00000000\n"
		++"x(0x00000003)  =  0x00000000\n"
		++"x(0x00000004)  =  0x00000000\n"
		++"x(0x00000005)  =  0x00000000\n"
		++"x(0x00000006)  =  0x00000000\n"
		++"x(0x00000007)  =  0x00000000\n"
		++"x(0x00000008)  =  0x00000000\n"
		++"x(0x00000009)  =  0x00000000\n"
		++"x(0x0000000a)  =  0x00000000\n"
		++"x(0x0000000b)  =  0x00000000\n"
		++"x(0x0000000c)  =  0x00000000\n"
		++"x(0x0000000d)  =  0x00000000\n"
		++"x(0x0000000e)  =  0x00000000\n"
		++"x(0x0000000f)  =  0x00000000\n"
		++"x(0x00000010)  =  0x00000000\n"
		++"x(0x00000011)  =  0x00000000\n"
		++"x(0x00000012)  =  0x00000000\n"
		++"x(0x00000013)  =  0x00000000\n"
		++"x(0x00000014)  =  0x00000000\n"
		++"x(0x00000015)  =  0x00000000\n"
		++"x(0x00000016)  =  0x00000000\n"
		++"x(0x00000017)  =  0x00000000\n"
		++"x(0x00000018)  =  0x00000000\n"
		++"x(0x00000019)  =  0x00000000\n"
		++"x(0x0000001a)  =  0x00000000\n"
		++"x(0x0000001b)  =  0x00000000\n"
		++"x(0x0000001c)  =  0x00000000\n"
		++"x(0x0000001d)  =  0x00000000\n"
		++"x(0x0000001e)  =  0x00000000\n"
		++"x(0x0000001f)  =  0x0cafe000\n"
		++">>> ")==serialOutput.result,"have current output")
}

class WithMonitorEJRTest extends CoreTestBase(c=>new WithMonitorEJRTester(c),false){}