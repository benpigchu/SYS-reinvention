package reinventation_core

import chisel3._
import chisel3.util._

class MemIO extends Bundle{
	val ready=Input(Bool())
	val iaddr=Output(UInt(34.W))
	val idata=Input(UInt(32.W))
	val access=Output(Bool())
	val rwtype=Output(UInt(MemOpSignal.width))
	val rwaddr=Output(UInt(34.W))
	val rdata=Input(UInt(32.W))
	val wdata=Output(UInt(32.W))
	val rwwidth=Output(UInt(MemWidthSignal.width))
}

class DebugIO extends Bundle{
	val regs=Output(Vec(32,UInt(32.W)))
}

class CoreIO extends Bundle{
	val mem=new MemIO
	val debug=new DebugIO
}

class Core extends Module{
	val NOP="b00000000000000000000000000010011".U(32.W)
	val io=IO(new CoreIO)
	//-------Declares
	//regs
	val pc=RegInit(0.U(32.W))
	val idInvalid=RegInit(false.B)
	val idInst=RegInit(NOP)
	val idPc=RegInit(0.U(32.W))
	val exInvalid=RegInit(false.B)
	val exPc=RegInit(0.U(32.W))
	val exInst=RegInit(NOP)
	val exImm=RegInit(0.U(32.W))
	val exSignal=RegInit(0.U.asTypeOf(new ControlSignals))
	val exRawDataA=RegInit(0.U(32.W))
	val exRawDataB=RegInit(0.U(32.W))
	val meInvalid=RegInit(false.B)
	val mePc=RegInit(0.U(32.W))
	val meInst=RegInit(NOP)
	val meImm=RegInit(0.U(32.W))
	val meSignal=RegInit(0.U.asTypeOf(new ControlSignals))
	val meDataA=RegInit(0.U(32.W))
	val meDataB=RegInit(0.U(32.W))
	val meALUResult=RegInit(0.U(32.W))
	val wbInvalid=RegInit(false.B)
	val wbPc=RegInit(0.U(32.W))
	val wbInst=RegInit(NOP)
	val wbImm=RegInit(0.U(32.W))
	val wbSignal=RegInit(0.U.asTypeOf(new ControlSignals))
	val wbDataA=RegInit(0.U(32.W))
	val wbDataB=RegInit(0.U(32.W))
	val wbALUResult=RegInit(0.U(32.W))
	val wbLoadedData=RegInit(0.U(32.W))
	//should we stall and keep the status in this stage?
	val pcStall=Wire(Bool())
	val idStall=Wire(Bool())
	val exStall=Wire(Bool())
	val meStall=Wire(Bool())
	val wbStall=Wire(Bool())
	//branch predict related
	val idExpectedTarget=Wire(UInt(32.W))
	val idExpectedBranch=Wire(Bool())
	val exBranchTarget=Wire(UInt(32.W))
	val exPredictUnsuccess=Wire(Bool())
	printf(p"----[stage]-----------------------------\n")
	//-------IF
	printf(p"----[branch] idExpectedBranch:$idExpectedBranch idExpectedTarget:0x${Hexadecimal(idExpectedTarget)}\n")
	printf(p"----[branch] exPredictUnsuccess:$exPredictUnsuccess exBranchTarget:0x${Hexadecimal(exBranchTarget)}\n")
	val stalledPcInvalid=idExpectedBranch|exPredictUnsuccess
	pcStall:=(!io.mem.ready)|(idStall&(!stalledPcInvalid))
	val nextPc=Mux(pcStall,pc,Mux(exPredictUnsuccess,exBranchTarget,Mux(idExpectedBranch,idExpectedTarget,pc+4.U)))
	printf(p"----[pc] nextPc:0x${Hexadecimal(nextPc)}\n")
	pc:=nextPc
	io.mem.iaddr:=Cat(0.U(2.W),pc)//we do not have mmu now
	//-------ID
	val haveMemWriteToDo=Wire(Bool())
	idStall:=exStall|((!idInvalid)&haveMemWriteToDo)//TODO
	idInvalid:=(idStall&idInvalid)|(pcStall&(!idStall))|((!idStall)&(idExpectedBranch|exPredictUnsuccess))
	idInst:=Mux(idStall,idInst,io.mem.idata)
	idPc:=Mux(idStall,idPc,pc)
	val idRegA=idInst(19,15)
	val idRegB=idInst(24,20)
	val control=Module(new Control)
	control.io.inst:=idInst
	val idSignal=control.io.signal
	val idImm=ImmGen(idSignal.immType,idInst)
	val regfile=Module(new RegFile)
	regfile.io.readRegA:=idRegA
	regfile.io.readRegB:=idRegB
	idExpectedTarget:=idPc+idImm
	//for jalr,always wait to ex
	//for jar,always predicted correct
	//for b,expect success when jumping back(imm(31)==inst(31))
	idExpectedBranch:=(!idStall)&(!idInvalid)&(idSignal.jal|(idSignal.branch&idInst(31)))
	//-------EX
	//also calculate address for me
	//also calculate jump target
	val useAfterLoadHazard=Wire(Bool())
	exStall:=meStall|useAfterLoadHazard//TODO
	exInvalid:=(exStall&exInvalid)|(idStall&(!exStall))|((!exStall)&exPredictUnsuccess)|((idInvalid&(!exStall)))
	exPc:=Mux(exStall,exPc,idPc)
	exInst:=Mux(exStall,exInst,idInst)
	exImm:=Mux(exStall,exImm,idImm)
	exSignal:=Mux(exStall,exSignal,idSignal)
	val exDataA=Wire(UInt(32.W))
	val exDataB=Wire(UInt(32.W))
	exRawDataA:=Mux(exStall,exDataA,regfile.io.readDataA)
	exRawDataB:=Mux(exStall,exDataB,regfile.io.readDataB)
	val exRegA=exInst(19,15)
	val exRegB=exInst(24,20)
	val alu=Module(new ALU)
	alu.io.op:=exSignal.aluOp
	import ALUASourceSignal._
	alu.io.inputA:=MuxLookup(exSignal.aluASource,0.U,Seq(
		A_REGA->exDataA,
		A_PC->exPc
	))
	import ALUBSourceSignal._
	alu.io.inputB:=MuxLookup(exSignal.aluBSource,0.U,Seq(
		B_REGB->exDataB,
		B_IMM->exImm
	))
	//for jalr,use alu output,always predicted incorrect
	//for jar,always predicted correct
	//for b,decide predict correctness with alu output
	val exBranchSucess=(alu.io.output=/=0.U)
	printf(p"----[ex] exImm:0x${Hexadecimal(exImm)} alu.io.op:${Hexadecimal(alu.io.op)}\n")
	printf(p"----[ex] alu.io.inputA:0x${Hexadecimal(alu.io.inputA)} alu.io.inputB:0x${Hexadecimal(alu.io.inputB)}\n")
	printf(p"----[ex] alu.io.output:0x${Hexadecimal(alu.io.output)} exBranchSucess:$exBranchSucess\n")
	printf(p"----[ex] inst:${Binary(exInst)}\n")
	exBranchTarget:=Mux(exSignal.jalr,Cat(alu.io.output(31,1),0.U(1.W)),Mux(exBranchSucess,exPc+exImm,exPc+4.U))
	exPredictUnsuccess:=(!exStall)&(!exInvalid)&(exSignal.jalr|(exSignal.branch&(exBranchSucess=/=exInst(31))))
	//-------ME
	meStall:=wbStall//TODO
	meInvalid:=(meStall&meInvalid)|(exStall&(!meStall))|((exInvalid&(!meStall)))
	mePc:=Mux(meStall,mePc,exPc)
	meInst:=Mux(meStall,meInst,exInst)
	meImm:=Mux(meStall,meImm,exImm)
	meSignal:=Mux(meStall,meSignal,exSignal)
	meDataA:=Mux(meStall,meDataA,exDataA)
	meDataB:=Mux(meStall,meDataB,exDataB)
	meALUResult:=Mux(meStall,meALUResult,alu.io.output)
	val meRegDest=meInst(11,7)
	printf(p"----[me] inst ${Binary(meInst)} access ${meSignal.accessMem} type ${meSignal.memOp}\n")
	printf(p"----[me] imm ${Binary(meImm)}\n")
	io.mem.access:=(!meInvalid)&meSignal.accessMem
	io.mem.rwtype:=meSignal.memOp
	io.mem.rwaddr:=meALUResult
	io.mem.wdata:=meDataB
	io.mem.rwwidth:=meSignal.memWidth
	val meRawLoadedData=io.mem.rdata
	import MemWidthSignal._
	val meLoadedData=MuxLookup(meSignal.memWidth,0.U,Seq(
		W_W->meRawLoadedData,
		W_H->Cat(Fill(16,Mux(meSignal.memLoadUnsigned,0.U,meRawLoadedData(15))),meRawLoadedData(15,0)),
		W_B->Cat(Fill(24,Mux(meSignal.memLoadUnsigned,0.U,meRawLoadedData(7))),meRawLoadedData(7,0))
	))
	//-------WB
	wbStall:=(!io.mem.ready)//currently we block the whole pipeline when io is not ready
	wbInvalid:=(wbStall&wbInvalid)|(meStall&(!wbStall))|(meInvalid&(!wbStall))
	wbPc:=Mux(wbStall,wbPc,mePc)
	wbInst:=Mux(wbStall,wbInst,meInst)
	wbImm:=Mux(wbStall,wbImm,meImm)
	wbSignal:=Mux(wbStall,wbSignal,meSignal)
	wbDataA:=Mux(wbStall,wbDataA,meDataA)
	wbDataB:=Mux(wbStall,wbDataB,meDataB)
	wbALUResult:=Mux(wbStall,wbALUResult,meALUResult)
	wbLoadedData:=Mux(wbStall,wbLoadedData,meLoadedData)
	regfile.io.writeEnable:=wbSignal.writeBack&(!wbInvalid)
	val wbRegDest=wbInst(11,7)
	regfile.io.writeReg:=wbRegDest
	import WritebackSourceSignal._
	val wbRegData=MuxLookup(wbSignal.writeBackSource,wbALUResult,Seq(
		WB_ALU->wbALUResult,
		WB_MEM->wbLoadedData,
		WB_PC4->(wbPc+4.U)
	))
	regfile.io.writeData:=wbRegData
	//-------Forward+Hazard
	val exNeedMeForwardA=exSignal.useRegA&(!meInvalid)&meSignal.writeBack&(meRegDest===exRegA)&(!meSignal.accessMem)&(exRegA=/=0.U)
	val exNeedMeForwardB=exSignal.useRegB&(!meInvalid)&meSignal.writeBack&(meRegDest===exRegB)&(!meSignal.accessMem)&(exRegB=/=0.U)
	val exNeedWbForwardA=exSignal.useRegA&(!wbInvalid)&wbSignal.writeBack&(wbRegDest===exRegA)&(exRegA=/=0.U)
	val exNeedWbForwardB=exSignal.useRegB&(!wbInvalid)&wbSignal.writeBack&(wbRegDest===exRegB)&(exRegB=/=0.U)
	exDataA:=Mux(exNeedMeForwardA,meALUResult,Mux(exNeedWbForwardA,wbRegData,exRawDataA))
	exDataB:=Mux(exNeedMeForwardB,meALUResult,Mux(exNeedWbForwardB,wbRegData,exRawDataB))
	printf(p"----[forword] regA:$exRegA dataA:${Hexadecimal(exDataA)} use:${exSignal.useRegA}\n")
	printf(p"----[forword] regB:$exRegB dataB:${Hexadecimal(exDataB)} use:${exSignal.useRegB}\n")
	printf(p"----[forword] me:$meRegDest result:${Hexadecimal(meALUResult)} use:${meSignal.writeBack}\n")
	printf(p"----[forword] wb:$wbRegDest result:${Hexadecimal(wbRegData)} use:${wbSignal.writeBack}\n")
	val useAfterLoadHazardA=exSignal.useRegA&(!meInvalid)&meSignal.writeBack&(meRegDest===exRegA)&(meSignal.accessMem)&(exRegA=/=0.U)
	val useAfterLoadHazardB=exSignal.useRegB&(!meInvalid)&meSignal.writeBack&(meRegDest===exRegB)&(meSignal.accessMem)&(exRegB=/=0.U)
	useAfterLoadHazard:=useAfterLoadHazardA|useAfterLoadHazardB
	//-------fence.i
	import MemOpSignal._
	haveMemWriteToDo:=idSignal.fenceI&((exSignal.accessMem&(exSignal.memOp===M_STORE))|(meSignal.accessMem&(meSignal.memOp===M_STORE)))
	//-------Debug
	io.debug.regs:=regfile.io.debug
	printf(p"----[status] stall: pc=$pcStall id=$idStall ex=$exStall me=$meStall wb=$wbStall\n")
	printf(p"----[status] invalid: id=$idInvalid ex=$exInvalid me=$meInvalid wb=$wbInvalid\n")
	printf(p"----[status] pc: pc=0x${Hexadecimal(pc)} id=0x${Hexadecimal(idPc)} ex=0x${Hexadecimal(exPc)} me=0x${Hexadecimal(mePc)} wb=0x${Hexadecimal(wbPc)}\n")
}