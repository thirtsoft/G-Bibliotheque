package com.biblio.loan;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.biblio.book.Book;
import com.biblio.customer.Customer;

@RestController
@RequestMapping("/rest/loan/api")
public class LoanRestController {
	
	public static final Logger LOGGER = LoggerFactory.getLogger(LoanRestController.class);
	
	@Autowired
	private LoanServiceImpl loanService;
	
	/**
	 * Retourne l'historique des prets en cours dans a bibliothèque jusqu'à une certaine
	 * date maximale
	 * @param maxEndDateStr
	 * @return
	 */
	
	@GetMapping("/maxEndDate")
	public ResponseEntity<List<LoanDTO>> searchAllBooksLoanBeofreThisDate(@RequestParam("date") String maxEndDateStr) {
		List<Loan> loans = loanService.findAllLoansByEndDateBefore(LocalDate.parse(maxEndDateStr));
		loans.removeAll(Collections.singleton(null));
		List<LoanDTO> loanInfosDtos = mapLoanDtosFromLoans(loans);
		return new ResponseEntity<List<LoanDTO>>(loanInfosDtos, HttpStatus.OK);
	}
	
	/**
	 * Retourne la liste des prets en cours d'un client
	 * @param email
	 * @return
	 */
	@GetMapping("/customerLoans")
	public ResponseEntity<List<LoanDTO>> searchAllOpenedLoansOfThisCustomer(@RequestParam("email") String email) {
		List<Loan> loans = loanService.getAllOpenLoansOfThisCustomer(email, LoanStatus.OPEN);
		// on retire tous les élts null que peut contenir cette liste => pour éviter les NPE par la suite.         
		loans.removeAll(Collections.singleton(null));        
		List<LoanDTO> loanInfosDtos = mapLoanDtosFromLoans(loans);        
		return new ResponseEntity<List<LoanDTO>>(loanInfosDtos, HttpStatus.OK); 
	}
	
	/**
	 * Ajout un nouveau pret dans la base de données H2
	 * @param simpleLoanDTORequest
	 * @param uriComponentsBuilder
	 * @return
	 */
	
	@PostMapping("/addLoan")
	public ResponseEntity<Boolean> createNewLoan(@RequestBody SimpleLoanDTO simpleLoanDTORequest,
			UriComponentsBuilder uriComponentsBuilder) {
		boolean isLoanExists = loanService.checkIfLoanExists(simpleLoanDTORequest);
		if(isLoanExists) {
			return new ResponseEntity<Boolean>(false, HttpStatus.CONFLICT);
		}
		Loan LoanRequest = mapSimpleLoanDTOToLoan(simpleLoanDTORequest);
		Loan loan = loanService.saveLoan(LoanRequest);
		if (loan != null) {
			return new ResponseEntity<Boolean>(true, HttpStatus.CREATED);
		}
		return new ResponseEntity<Boolean>(false, HttpStatus.NOT_MODIFIED);
		
	}
	
	/**
	 * Cloturer le pret de livre d'un client
	 * 
	 * @param simpleLoanDTORequest
	 * @param uriComponentsBuilder
	 * @return
	 */
	@PostMapping("/closeLoan")
	public ResponseEntity<Boolean> closeLoan(@RequestBody SimpleLoanDTO simpleLoanDTORequest,
			UriComponentsBuilder uriComponentsBuilder) {
		Loan existingLoan = loanService.getOpenedLoan(simpleLoanDTORequest);
		if(existingLoan == null) {
			return new ResponseEntity<Boolean>(false, HttpStatus.NO_CONTENT);
		}
		existingLoan.setStatus(LoanStatus.CLOSE);
		Loan loan = loanService.saveLoan(existingLoan);
		if (loan != null) {
			return new ResponseEntity<Boolean>(true, HttpStatus.OK);
		}
		return new ResponseEntity<Boolean>(HttpStatus.NOT_MODIFIED);
 	}
	
	/**
	 * Transforme une liste d'entités Lo Loan an liste LoanDTO
	 * 
	 * @param loans
	 * @return
	 */
	private List<LoanDTO> mapLoanDtosFromLoans(List<Loan> loans) {
		Function<Loan, LoanDTO> mapperFunction = (loan)-> {
			//dans loanDTO on n'ajoute que les données nécessaires
			LoanDTO loanDTO = new LoanDTO();
			loanDTO.getBookDTO().setId(loan.getPk().getBook().getId());
			loanDTO.getBookDTO().setIsbn(loan.getPk().getBook().getIsbn());
			loanDTO.getBookDTO().setTitle(loan.getPk().getBook().getTitle());
			
			loanDTO.getCustomerDTO().setId(loan.getPk().getCustomer().getId());
			
			loanDTO.getCustomerDTO().setFirstName(loan.getPk().getCustomer().getFirstName());
			loanDTO.getCustomerDTO().setLastName(loan.getPk().getCustomer().getLastName());
			loanDTO.getCustomerDTO().setEmail(loan.getPk().getCustomer().getEmail());
			
			loanDTO.setLoanBeginDate(loan.getBeginDate());
			loanDTO.setLoanEndDate(loan.getEndDate());
			return loanDTO;
		};
		
		if (!CollectionUtils.isEmpty(loans)) {
			return loans.stream().map(mapperFunction).sorted().collect(Collectors.toList());
		}
		
		return null;
	}
	/**
	 * Transforme un SimpleLoanDTO en Loan avec les données minimaliste nécessaire
	 * 
	 * @param simpleLoanDTORequest
	 * @return
	 */
	private Loan mapSimpleLoanDTOToLoan(SimpleLoanDTO simpleLoanDTO) {
		Loan loan = new Loan();
		
		Book book = new Book();
		book.setId(simpleLoanDTO.getBookId());
		
		Customer customer = new Customer();
		customer.setId(simpleLoanDTO.getCustomerId());
		
		LoanId loanId = new LoanId(book, customer);
		
		loan.setPk(loanId);
		loan.setBeginDate(simpleLoanDTO.getBeginDate());
		loan.setEndDate(simpleLoanDTO.getEndDate());
		loan.setStatus(LoanStatus.OPEN);
		
		return loan;
	}

	
}
