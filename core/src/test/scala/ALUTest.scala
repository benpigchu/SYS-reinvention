import scala.util._
import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import reinventation_core._

class ALUTester(alu:ALU)extends PeekPokeTester(alu){
	import ALUOp._
	//scala do not have unsigned integar
	val ops=Map[UInt,(Int,Int)=>Int](
		OP_ADD->((a:Int,b:Int)=>(a+b)),
		OP_SLL->((a:Int,b:Int)=>(a<<(b&31))),
		OP_SLT->((a:Int,b:Int)=>(if(a<b)1 else 0)),
		OP_SLTU->((a:Int,b:Int)=>(if(Integer.compareUnsigned(a,b)<0)1 else 0)),
		OP_XOR->((a:Int,b:Int)=>(a^b)),
		OP_SRL->((a:Int,b:Int)=>(a>>>(b&31))),
		OP_OR->((a:Int,b:Int)=>(a|b)),
		OP_AND->((a:Int,b:Int)=>(a&b)),
		OP_SUB->((a:Int,b:Int)=>(a-b)),
		OP_SRA->((a:Int,b:Int)=>(a>>(b&31))),
		OP_SGE->((a:Int,b:Int)=>(if(a>b)1 else 0)),
		OP_SGEU->((a:Int,b:Int)=>(if(Integer.compareUnsigned(a,b)>0)1 else 0)),
		OP_SEQ->((a:Int,b:Int)=>(if(a==b)1 else 0)),
		OP_SNE->((a:Int,b:Int)=>(if(a!=b)1 else 0))
	)
	for((op,func)<-ops){
		val inputA=rnd.nextInt()
		val inputB=rnd.nextInt()
		poke(alu.io.op,op)
		poke(alu.io.inputA,Integer.toUnsignedLong(inputA))
		poke(alu.io.inputB,Integer.toUnsignedLong(inputB))
		expect(alu.io.output,Integer.toUnsignedLong(func(inputA,inputB)))
	}
}

class ALUTest extends ChiselFlatSpec{
	"ALU" should "works" in {
		iotesters.Driver.execute(Array("--is-verbose"),()=>new ALU){
			c=>new ALUTester(c)
		}should be(true)
	}
}