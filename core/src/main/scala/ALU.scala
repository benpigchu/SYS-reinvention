package reinventation_core

import chisel3._
import chisel3.util._

object ALUOp{//=(Rtype?inst[30]:'0')++inst[14:12]
	val ADD="b0000".U(4.W)
	val SLL="b0001".U(4.W)
	val SLT="b0010".U(4.W)
	val SLTU="b0011".U(4.W)
	val XOR="b0100".U(4.W)
	val SRL="b0101".U(4.W)
	val OR="b0110".U(4.W)
	val AND="b0111".U(4.W)
	val SUB="b1000".U(4.W)
	val SRA="b1100".U(4.W)
}

class ALUIO extends Bundle{
	val op=Input(UInt(4.W))
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
		ADD->(a+b),
		SLL->(a<<b(4,0)),
		SLT->Mux(a.asSInt<b.asSInt,1.U,0.U),
		SLTU->Mux(a<b,1.U,0.U),
		XOR->(a^b),
		SRL->(a>>b(4,0)),
		OR->(a|b),
		AND->(a&b),
		SUB->(a-b),
		SRA->((a.asSInt>>b(4,0)).asUInt)
	))
}