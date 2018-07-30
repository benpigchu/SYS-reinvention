package reinventation_core

import chisel3._
import chisel3.util._

object ALUOp{
	val width=4.W
	val OP_ADD="b0000".U(width)
	val OP_SLL="b0001".U(width)
	val OP_SLT="b0010".U(width)
	val OP_SLTU="b0011".U(width)
	val OP_XOR="b0100".U(width)
	val OP_SRL="b0101".U(width)
	val OP_OR="b0110".U(width)
	val OP_AND="b0111".U(width)
	val OP_SUB="b1000".U(width)
	val OP_SRA="b1100".U(width)
	val OP_SGE="b1010".U(width)
	val OP_SGEU="b1011".U(width)
	val OP_SEQ="b1001".U(width)
	val OP_SNE="b1101".U(width)
	val OP_X="b1111".U(width)
}

class ALUIO extends Bundle{
	val op=Input(UInt(ALUOp.width))
	val inputA=Input(UInt(32.W))
	val inputB=Input(UInt(32.W))
	val output=Output(UInt(32.W))
}

class ALU extends Module{
	import ALUOp._
	val io=IO(new ALUIO)
	val a=io.inputA
	val b=io.inputB
	io.output:=MuxLookup(io.op,0.U(32.W),Seq(
		OP_ADD->(a+b),
		OP_SLL->(a<<b(4,0)),
		OP_SLT->Mux(a.asSInt<b.asSInt,1.U,0.U),
		OP_SLTU->Mux(a<b,1.U,0.U),
		OP_XOR->(a^b),
		OP_SRL->(a>>b(4,0)),
		OP_OR->(a|b),
		OP_AND->(a&b),
		OP_SUB->(a-b),
		OP_SRA->((a.asSInt>>b(4,0)).asUInt),
		OP_SGE->Mux(a.asSInt>b.asSInt,1.U,0.U),
		OP_SGEU->Mux(a>b,1.U,0.U),
		OP_SEQ->Mux(a===b,1.U,0.U),
		OP_SNE->Mux(a=/=b,1.U,0.U)
	))
}