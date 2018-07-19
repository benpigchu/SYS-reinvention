import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class RegFileTester(reg:RegFile)extends PeekPokeTester(reg){
	poke(reg.io.readRegA,0)
	poke(reg.io.readRegB,1)
	expect(reg.io.readDataA,0)
	expect(reg.io.readDataB,0)
	step(1)
	poke(reg.io.writeEnable,true)
	poke(reg.io.writeData,10086)
	expect(reg.io.readDataA,0)
	expect(reg.io.readDataB,0)
	step(1)
	poke(reg.io.writeReg,1)
	expect(reg.io.readDataA,0)
	expect(reg.io.readDataB,10086)
	step(1)
	poke(reg.io.writeEnable,false)
	poke(reg.io.readRegA,1)
	poke(reg.io.readRegB,31)
	expect(reg.io.readDataA,10086)
	expect(reg.io.readDataB,0)
}

class RegFileTest extends ChiselFlatSpec{
	"RegFile" should "works" in {
		iotesters.Driver.execute(Array("--is-verbose"),()=>new RegFile){
			c=>new RegFileTester(c)
		}should be(true)
	}
}