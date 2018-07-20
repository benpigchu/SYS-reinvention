package reinventation_core

import chisel3._
import chisel3.util._

class ControlIO extends Bundle{
	val inst=Input(UInt(32.W))
	val signal=Output(new ControlSignals)
}

class ControlSignals extends Bundle{
	// legal instruction?
	val legal=Bool()
	// use source regs?
	val useRegA=Bool()
	val useRegB=Bool()
	// generate imm
	val immType=UInt(ImmTypeSignal.width)
	// put two number in the alu
	val aluOp=UInt(ALUOp.width)
	val aluASource=UInt(ALUASourceSignal.width)
	val aluBSource=UInt(ALUBSourceSignal.width)
	// write back to regs?
	val writeBack=Bool()
}

object DecodeData{
	import BoolSignal._
	import ALUASourceSignal._
	import ALUBSourceSignal._
	import ImmTypeSignal._
	import ALUOp._
	import InstructionPatterns._
	// List(legal,useRegA,useRegB,immType,aluOp,aluASource,aluBSource,writeBack)
	val default=List(F,F,F,IMM_S,OP_ADD,A_0,B_0,F)
	val table=Array(
		//--------Integer Computation
		LUI->List(T,F,F,IMM_U,OP_ADD,A_0,B_IMM,T),
		AUIPC->List(T,F,F,IMM_U,OP_ADD,A_PC,B_IMM,T),
		ADDI->List(T,T,F,IMM_I,OP_ADD,A_REGA,B_IMM,T),
		SLTI->List(T,T,F,IMM_I,OP_SLT,A_REGA,B_IMM,T),
		SLTIU->List(T,T,F,IMM_I,OP_SLTU,A_REGA,B_IMM,T),
		XORI->List(T,T,F,IMM_I,OP_XOR,A_REGA,B_IMM,T),
		ORI->List(T,T,F,IMM_I,OP_OR,A_REGA,B_IMM,T),
		ANDI->List(T,T,F,IMM_I,OP_AND,A_REGA,B_IMM,T),
		SLLI->List(T,T,F,IMM_I,OP_SLL,A_REGA,B_IMM,T),
		SRLI->List(T,T,F,IMM_I,OP_SRL,A_REGA,B_IMM,T),
		SRAI->List(T,T,F,IMM_I,OP_SRA,A_REGA,B_IMM,T),
		ADD->List(T,T,T,IMM_I,OP_ADD,A_REGA,B_REGB,T),
		SUB->List(T,T,T,IMM_I,OP_SUB,A_REGA,B_REGB,T),
		SLL->List(T,T,T,IMM_I,OP_SLL,A_REGA,B_REGB,T),
		SLT->List(T,T,T,IMM_I,OP_SLT,A_REGA,B_REGB,T),
		SLTU->List(T,T,T,IMM_I,OP_SLTU,A_REGA,B_REGB,T),
		XOR->List(T,T,T,IMM_I,OP_XOR,A_REGA,B_REGB,T),
		SRL->List(T,T,T,IMM_I,OP_SRL,A_REGA,B_REGB,T),
		SRA->List(T,T,T,IMM_I,OP_SRA,A_REGA,B_REGB,T),
		OR->List(T,T,T,IMM_I,OP_OR,A_REGA,B_REGB,T),
		AND->List(T,T,T,IMM_I,OP_AND,A_REGA,B_REGB,T)
	)
}

class Control extends Module{
	val io=IO(new ControlIO)
	// `ListLookup` is not well documented, but can be found in riscv-boom or riscv-mini
	// It works similar with MuxLookup, but for List[Bits] instead of Bits
	// Since we do not have bundle literal, this is useful to select bundle constants
	val signals=ListLookup(io.inst,DecodeData.default,DecodeData.table)
	io.signal.legal:=signals(0)
	io.signal.useRegA:=signals(1)
	io.signal.useRegB:=signals(2)
	io.signal.immType:=signals(3)
	io.signal.aluOp:=signals(4)
	io.signal.aluASource:=signals(5)
	io.signal.aluBSource:=signals(6)
	io.signal.writeBack:=signals(7)
}