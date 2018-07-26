import scala.collection.mutable._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class CoreTester(core:Core)extends PeekPokeTester(core){
	val mem=new ArrayBuffer[Byte](0x800000)
	poke(core.io.mem.ready,true)
	poke(core.io.mem.idata,0x00000013L)//NOP
	step(5)
	expect(core.io.debug.regs(0),0)
}

class CoreTest extends ChiselFlatSpec{
	"Core" should "works" in {
		iotesters.Driver.execute(Array("--is-verbose"),()=>new Core){
			c=>new CoreTester(c)
		}should be(true)
	}
}