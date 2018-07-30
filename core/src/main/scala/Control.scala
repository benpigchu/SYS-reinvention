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
	// access mem
	val accessMem=Bool()
	val memOp=UInt(MemOpSignal.width)
	val memWidth=UInt(MemWidthSignal.width)
	val memLoadUnsigned=Bool()
	// write back to regs?
	val writeBack=Bool()
	val writeBackSource=UInt(WritebackSourceSignal.width)
	// branch?
	val jal=Bool()
	val jalr=Bool()
	val branch=Bool()
}

object DecodeData{
	import BoolSignal._
	import ALUASourceSignal._
	import ALUBSourceSignal._
	import ImmTypeSignal._
	import ALUOp._
	import MemOpSignal._
	import MemWidthSignal._
	import WritebackSourceSignal._
	import InstructionPatterns._
	//               legal
	//               | useRegA           aluASource              memWidth
	//               | | useRegB         |      aluBSource       |   memLoadUnsigned
	//               | | | immType 	     |      |      accessMem |   | writeBack
	//               | | | |	  aluOp  |      |      | memOp   |   | | writeBackSource
	//               | | | |	  |      |      |      | |       |   | | |      jal
	//               | | | |	  |      |      |      | |       |   | | |      | jalr
	//               | | | |	  |      |      |      | |       |   | | |      | | branch
	//               | | | |	  |      |      |      | |       |   | | |      | | |
	val default=List(F,F,F,IMM_X,OP_X   ,A_X   ,B_X   ,F,M_X    ,W_X,X,X,WB_X  ,F,F,F)
	val table=Array(
		//--------Integer Computation
		LUI   ->List(T,F,F,IMM_U,OP_ADD ,A_0   ,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		AUIPC ->List(T,F,F,IMM_U,OP_ADD ,A_PC  ,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		ADDI  ->List(T,T,F,IMM_I,OP_ADD ,A_REGA,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SLTI  ->List(T,T,F,IMM_I,OP_SLT ,A_REGA,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SLTIU ->List(T,T,F,IMM_I,OP_SLTU,A_REGA,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		XORI  ->List(T,T,F,IMM_I,OP_XOR ,A_REGA,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		ORI   ->List(T,T,F,IMM_I,OP_OR  ,A_REGA,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		ANDI  ->List(T,T,F,IMM_I,OP_AND ,A_REGA,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SLLI  ->List(T,T,F,IMM_I,OP_SLL ,A_REGA,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SRLI  ->List(T,T,F,IMM_I,OP_SRL ,A_REGA,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SRAI  ->List(T,T,F,IMM_I,OP_SRA ,A_REGA,B_IMM ,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		ADD   ->List(T,T,T,IMM_X,OP_ADD ,A_REGA,B_REGB,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SUB   ->List(T,T,T,IMM_X,OP_SUB ,A_REGA,B_REGB,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SLL   ->List(T,T,T,IMM_X,OP_SLL ,A_REGA,B_REGB,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SLT   ->List(T,T,T,IMM_X,OP_SLT ,A_REGA,B_REGB,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SLTU  ->List(T,T,T,IMM_X,OP_SLTU,A_REGA,B_REGB,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		XOR   ->List(T,T,T,IMM_X,OP_XOR ,A_REGA,B_REGB,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SRL   ->List(T,T,T,IMM_X,OP_SRL ,A_REGA,B_REGB,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		SRA   ->List(T,T,T,IMM_X,OP_SRA ,A_REGA,B_REGB,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		OR    ->List(T,T,T,IMM_X,OP_OR  ,A_REGA,B_REGB,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		AND   ->List(T,T,T,IMM_X,OP_AND ,A_REGA,B_REGB,F,M_X    ,W_X,X,T,WB_ALU,F,F,F),
		//--------Load and Store
		LB    ->List(T,T,F,IMM_I,OP_ADD ,A_REGA,B_IMM ,T,M_LOAD ,W_B,F,T,WB_MEM,F,F,F),
		LH    ->List(T,T,F,IMM_I,OP_ADD ,A_REGA,B_IMM ,T,M_LOAD ,W_H,F,T,WB_MEM,F,F,F),
		LW    ->List(T,T,F,IMM_I,OP_ADD ,A_REGA,B_IMM ,T,M_LOAD ,W_W,F,T,WB_MEM,F,F,F),
		LBU   ->List(T,T,F,IMM_I,OP_ADD ,A_REGA,B_IMM ,T,M_LOAD ,W_B,T,T,WB_MEM,F,F,F),
		LHU   ->List(T,T,F,IMM_I,OP_ADD ,A_REGA,B_IMM ,T,M_LOAD ,W_H,T,T,WB_MEM,F,F,F),
		SB    ->List(T,T,T,IMM_S,OP_ADD ,A_REGA,B_IMM ,T,M_STORE,W_B,X,F,WB_X  ,F,F,F),
		SH    ->List(T,T,T,IMM_S,OP_ADD ,A_REGA,B_IMM ,T,M_STORE,W_H,X,F,WB_X  ,F,F,F),
		SW    ->List(T,T,T,IMM_S,OP_ADD ,A_REGA,B_IMM ,T,M_STORE,W_W,X,F,WB_X  ,F,F,F),
		//--------Jump and Branch
		JAL   ->List(T,F,F,IMM_J,OP_ADD ,A_PC  ,B_IMM ,F,M_X    ,W_X,X,T,WB_PC4,T,F,F),
		JALR  ->List(T,T,F,IMM_I,OP_ADD ,A_REGA,B_IMM ,F,M_X    ,W_X,X,T,WB_PC4,F,T,F),
		BEQ   ->List(T,T,T,IMM_X,OP_SEQ ,A_REGA,B_REGB,F,M_X    ,W_X,X,F,WB_X  ,F,F,T),
		BNE   ->List(T,T,T,IMM_X,OP_SNE ,A_REGA,B_REGB,F,M_X    ,W_X,X,F,WB_X  ,F,F,T),
		BLT   ->List(T,T,T,IMM_X,OP_SLT ,A_REGA,B_REGB,F,M_X    ,W_X,X,F,WB_X  ,F,F,T),
		BGE   ->List(T,T,T,IMM_X,OP_SGE ,A_REGA,B_REGB,F,M_X    ,W_X,X,F,WB_X  ,F,F,T),
		BLTU  ->List(T,T,T,IMM_X,OP_SLTU,A_REGA,B_REGB,F,M_X    ,W_X,X,F,WB_X  ,F,F,T),
		BGEU  ->List(T,T,T,IMM_X,OP_SGEU,A_REGA,B_REGB,F,M_X    ,W_X,X,F,WB_X  ,F,F,T)
	)
}

class Control extends Module{
	val io=IO(new ControlIO)
	// `ListLookup` is not well documented, but can be found in riscv-boom or riscv-mini
	// It works similar with MuxLookup, but for List[Bits] instead of Bits
	// Since we do not have bundle literal, this is useful to select bundle constants
	val signals=ListLookup(io.inst,DecodeData.default,DecodeData.table)
	io.signal.legal          :=signals(0)
	io.signal.useRegA        :=signals(1)
	io.signal.useRegB        :=signals(2)
	io.signal.immType        :=signals(3)
	io.signal.aluOp          :=signals(4)
	io.signal.aluASource     :=signals(5)
	io.signal.aluBSource     :=signals(6)
	io.signal.accessMem      :=signals(7)
	io.signal.memOp          :=signals(8)
	io.signal.memWidth       :=signals(9)
	io.signal.memLoadUnsigned:=signals(10)
	io.signal.writeBack      :=signals(11)
	io.signal.writeBackSource:=signals(12)
	io.signal.jal            :=signals(13)
	io.signal.jalr           :=signals(14)
	io.signal.branch         :=signals(15)
}